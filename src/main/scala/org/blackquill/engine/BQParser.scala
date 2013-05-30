package org.blackquill.engine

// BlackQuill Copyright (C) 2013 set.minami<set.minami@gmail.com>
// License MIT see also LISENCE.txt
// BQParser.

import org.apache.commons.logging._
import scala.collection.immutable.List
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.Stack
import scala.collection.mutable.ListMap
import scala.collection.SortedSet
import scala.util.matching.Regex
import scala.xml._

import org.blackquill.engine._

class BQParser {
	private val log:Log = LogFactory.getLog(classOf[BQParser])

	private val Syntax = LinkedHashMap(
	"^(.*?\\\\,)((>.*?\\\\,)+?)\\\\,(.*?)$$" -> ("blockquote",surroundByBlockquoteTAG _),
	"^(.*?\\\\,)>(.*?)\\\\,\\\\,(.*?)$$" -> ("blockquote",surroundByGeneralTAG _),
	"^(.*?)((\\s+\\d+?\\.\\s.+?\\\\,\\s*?)+?)(.*?)$$" -> ("ol",surroundByListTAG _),
	"^(.*?)((\\s+(?:\\*|\\+|\\-)\\s.+?\\\\,\\s*?)+?)(.*?)$$" -> ("ul",surroundByListTAG _),
	"^(.*?)\\*\\*(.+?)\\*\\*(.*?)$$" -> ("em",surroundByGeneralTAG _),
	"^(.*?)\\*(.+?)\\*(.*?)$$" -> ("i",surroundByGeneralTAG _),
	"^(.*\\\\,)(.*?)\\\\,(\\-+|=+)\\s*\\\\,(.*)$$" -> ("h",surroundByHeadTAGUnderlineStyle _),
	"^(.*?)(#{1,6})\\s(.+?)(\\s#{1,6}?){0,1}\\\\,(.*?)$$" -> ("h",surroundByHeadTAG _)
	//"^(.*?)(\\\\,.+?\\\\,)(.*?)$$" -> ("p",surroundByAbstructTAG _)
	)
	
	private def surroundByBlockquoteTAG(doc:String, regex:String, TAG:String):String = {
	  val p = new Regex(regex, "before","inTAG","midInTag","following")
	  val m = p findFirstMatchIn(doc)
	  
	  var bef = ""
	  var fol = ""
	  var contentStr = ""
	  if(m != None){
	    val mid = m.get.group("inTAG")
	    log info "***-->" + mid

	    if(m.get.group("before") != None){bef = m.get.group("before")}else{bef = ""}
  		if(m.get.group("following") != None){fol = m.get.group("following")}else{fol = ""}
  		
  		log info "=>" + mid
		if(mid != ""){
		  val mat = """(>(.+?\\,))+?""".r.findAllMatchIn(mid)
		  
		  for(mt <- mat){
		    contentStr += mt.group(2)
		    log info "^^^" + mt
		  }
		}

		log info "-->" + contentStr
		return surroundByBlockquoteTAG(bef, regex, TAG) + 
				"<blockquote>" + contentStr + "</blockquote>" + surroundByBlockquoteTAG(fol, regex, TAG)
	  }
	  doc
	}

	private def surroundByListTAG(doc:String, regex:String, TAG:String):String = {
		val p = new Regex(regex, "before","elements","element","following")
	  	val m = p findFirstMatchIn(doc)
		var s = ""
		var bef = ""
		var fol = ""

	  	if(m != None){
  			if(m.get.group("before") != None){bef = m.get.group("before")}else{bef = ""}
  			if(m.get.group("following") != None){fol = m.get.group("following")}else{fol = ""}
			s = m.get.group("elements")
	  	}else{
	  	  return doc
	  	}

		
		var sign = ""
		var sp = ""
		val indentWidth = 4
		var styles:((Int) => String) = null
		
	  	TAG match{
	      case "ul"=>
	        sp = TAG
	        styles = (index:Int) => {
	          	  (index/indentWidth)%3 match{
	          	  	case 1 => "disc"
	          	  	case 2 => "circle"
	          	  	case 0 => "square"
	          	  	case _ => "---"
	          	  }
	        }
	        sign = "[\\*|\\+|\\-]"
	      case "ol"=>
	        sp = TAG
	        styles = (index:Int) => {
	        	  (index/indentWidth)%4 match{
	        	    case 1 => "decimal"
	        	    case 2 => "decimal-leading-zero"
	        	    case 3 => "upper-latin"
	        	    case 0 => "lower-latin"	        	    
	        	    case _ => "---"
	        	  }
	        }
	        sign = "\\d+?\\."
	    }
		
		var docList = List[String]()
		for(elem <- s"""(\\s+?$sign\\s.+?\\\\,)+?""".r.findAllMatchIn(s)){				 
			docList = elem.group(1)::docList
		}


		
	  	def _surroundByListTAG(doc:List[String],TAG:String,indent:Int):TreeNode[String] = {	
	  		var tree = new TreeNode[String]("")
	  	    if(doc.isEmpty){return tree}	  	  		  		
	  	
	  		tree.add(new TreeNode("<" + sp + s""" style=\"list-style-type:${styles(indent)}\">"""))
				  			  		var i = indent
			var list = List.empty[Tuple3[String,Int,String]]
	  		for(elem <- doc){
	  		  val m = s"""((\\s+?)$sign\\s(.+?)\\,)""".r.findFirstMatchIn(elem)
	  		  list = (m.get.group(1),m.get.group(2).size,m.get.group(3))::list
	  		}

			var restStr = List[String]()
			if(list.isEmpty){return new TreeNode("")
	  		}else{for(e <- list.reverse.tail){restStr = e._1::restStr}}
			
			restStr = restStr.reverse
			for(elem <- list.reverse){			
				if(elem._2 > i){			   
					tree.add(new TreeNode("<" + sp + s""" style=\"list-style-type:${styles(elem._2)}\">"""))
				}else if(elem._2 < i){					
					tree.add(new TreeNode[String](s"</$sp>"*((i - elem._2)/indentWidth)))
				}
				tree.add(new TreeNode[String](s"<$TAG>" + elem._3 + s"</$TAG>\\,"))	
				
				
				if(restStr.isEmpty){
					restStr = List[String]("")				  
				}else{
					restStr = restStr.tail
				}
				i = elem._2
			}
		tree.add(new TreeNode(s"</$sp>"*((i - indent)/indentWidth + 1)))
  		return tree
	  }
	  	val r1 = s"""(\\s*)${sign}.*?\\,""".r
	  	val wS1 = r1.findFirstMatchIn(s)
	  	var str = ""
	  	val r2 = s"""(\\s*)${sign}.*(\\\\,<br />.*?</blockquote>\\\\,)""".r
	  	val wS2 = r2.findFirstMatchIn(s)
	  	log debug "===>" + s
	  	
	  	var wS:Option[Regex.Match] = null
	  	if(wS2 != None){wS = wS2}else if(wS1 != None){wS = wS1}
	  	if(wS != None){
	  		for(e <- _surroundByListTAG(docList.reverse,"li",wS.get.group(1).size)){
	  		  str += e.toString()
	  		}
	  	  if(wS == wS2){str += wS.get.group(2)}
	  	  
	  	  log debug "---->" + str
	  	  surroundByListTAG(bef,regex,TAG) + str + surroundByListTAG(fol,regex,TAG)
	  	}else{doc}
	}
	
	private def surroundByHeadTAGUnderlineStyle(doc:String, regex:String, TAG:String):String = {
	  if(doc == ""){return doc}
	  
	  log debug "-->" + doc
	  val p = new Regex(regex, "before","inTAG","style","following")
	  val m = p findFirstMatchIn(doc)
	  var bef = ""
	  var fol = ""
	  var contentStr = ""
	  var headSign = TAG
	  if(m != None){
	    if(m.get.group("before") != None){bef = m.get.group("before")}else{bef = ""}
	    if(m.get.group("following") != None){fol = m.get.group("following")}else{fol = ""}
	    contentStr = m.get.group("inTAG")
	  
	    if(m.get.group("style").contains("-")){
	      headSign += "2"
	    }else{
	      headSign += "1"
	    }
	  
	    return surroundByHeadTAGUnderlineStyle(bef, regex, TAG) +
			  s"<$headSign>$contentStr</$headSign>\\," + surroundByHeadTAGUnderlineStyle(fol, regex, TAG)
	  }
	  doc
	}
	
	private def surroundByHeadTAG(doc:String, regex:String, TAG:String):String = {
	  	if(doc == ""){return doc}
	  	
	  	log debug "--> " + doc
	  	val p = new Regex(regex, "before","startHead","inTAG","endHead","following")
		val m = p findFirstMatchIn(doc)
	  	var contentStr = ""
		if(m != None){
	      var bef = ""
	      var fol = ""
	      
		  if(m.get.group("before") != None){bef = m.get.group("before")}else{bef = ""}
	      if(m.get.group("following") != None){fol = m.get.group("following")}else{fol = ""}
	      contentStr = m.get.group("inTAG")
	      
		  val headSize = m.get.group("startHead").size
		  val endHead = m.get.group("endHead")
		  log debug "-->" + endHead
		  val headTAG = TAG + headSize
		  if(endHead != null){
		    val m2 = """^\s(#+?)$$""".r("wantSize").findFirstMatchIn(endHead)
		    if(m2 != None){
		      val size = m2.get.group("wantSize")
		      if(size.size != headSize){
		        contentStr += " " + size
		        log warn "Curious header expression was found. " + 
		        s" BlackQuill represents this <$headTAG>$contentStr</$headTAG> ."
		      }
		    }
		  }
		  
		  return surroundByHeadTAG(bef,regex,TAG) +
				  s"<${headTAG}>$contentStr</${headTAG}>\\," +
				  surroundByHeadTAG(fol,regex,TAG)
		}
	  doc
	}
	
	private def surroundByGeneralTAG(doc:String, regex:String, TAG:String):String = {
	  if(doc == ""){return doc}
	  log debug doc
	  val p = new Regex(regex,"before","inTAG","following")
	  val m = p findFirstMatchIn(doc)
	  if(m != None){
	      var bef = ""
	      var fol = ""
		  if(m.get.group("before") != None){bef = m.get.group("before")}else{bef = ""}
	      if(m.get.group("following") != None){fol = m.get.group("following")}else{fol = ""}
		  return surroundByGeneralTAG(bef,regex,TAG) + 
				  s"<${TAG}>" + m.get.group("inTAG") + s"</${TAG}>" +
	    		  surroundByGeneralTAG(fol,regex,TAG)
	  }
	  doc
	}
	
	private def constructHEADER(doc:String):String = {
	  val headTAG = "head"
	  val titleTAG = "title"
	  var title = "NO TITLE"
	  
	  val p = new Regex("""(?i)#{1,6}\s(.+?)\\,""")
	  val m = p findFirstMatchIn(doc)
	  if(m != None){title = m.get.group(1)}
	  
	  s"<${headTAG}>\n<${titleTAG}>${title}</${titleTAG}>\n</${headTAG}>"
	}
	
	def toHTML(markdown:String):String = {
	  	val docType = "<!DOCTYPE html>"
	  	val bodyTAG = "body"
	  	val htmlTAG = "html"
	  	var md = markdown
		for(k <- Syntax keys){
		 md = Syntax(k)._2(md, k, Syntax(k)._1)
		}
		val header = constructHEADER(markdown)
		s"${docType}\n${header}\n<${htmlTAG}>\n<${bodyTAG}>\n${md}\n</${bodyTAG}>\n</${htmlTAG}>"
	}
	
	
}