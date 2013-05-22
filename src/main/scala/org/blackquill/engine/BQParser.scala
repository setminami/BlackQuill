package org.blackquill.engine

// BlackQuill Copyright (C) 2013 set.minami<set.minami@gmail.com>
// License MIT see also LISENCE.txt
// BQParser.

import org.apache.commons.logging._
import scala.collection.immutable.List
import scala.collection.mutable.LinkedHashMap
import scala.util.matching.Regex
import scala.xml._

import org.blackquill.engine._

class BQParser {
	private val log:Log = LogFactory.getLog(classOf[BQParser])

	private val Syntax = LinkedHashMap(
	"^(.*?)((\\s*\\*\\s.+?\\\\,)+)(.*?)$$" -> ("ul",surroundByListTAG _),
	"^(.*?)\\*(.+?)\\*(.*?)$$" -> ("i",surroundByAbstructTAG _),
	"^(.*?)\\*\\*(.+?)\\*\\*(.*?)$$" -> ("em",surroundByAbstructTAG _),
	"^(.*?)(#+)\\s(.+?)\\\\,(.*?)$$" -> ("h",surroundByHeadTAG _)
	//"^(.*?)(\\\\,.+?\\\\,)(.*?)$$" -> ("p",surroundByAbstructTAG _)
	)

/*
	private def controlStyle(style:List[String]):String = {
	  if(style.size == 0){
		  log warn "<ul> style: too many nests found where listed by by *"
		  System.exit(-1)
		  return ""
	  }else{return style.head}
	}
*/
	private def surroundByListTAG(doc:String, regex:String, TAG:String):String = {
		var styles = List[String]()
		var sign = ""
		var sp = ""
		val ulStyles = List[String]("disc","disc","disc","circle","circle","circle","square","square","square")
		val olStyles = List[String]("disc","circle","square")	  
	  	TAG match{
	      case "ul"=>
	        sp = TAG
	        styles = ulStyles
	        sign = "*"
	      case "ol"=>
	        sp = TAG
	        styles = olStyles
	        sign = "-"
	    }
		var tree = new TreeNode[String]("root")

	  	def _surroundByListTAG(doc:String, regex:String, TAG:String, style:List[String],indent:Int):String = {
	  		if(doc == ""){return ""}
	  		if(style.size == 0){
	  		    log warn s"<$sp> style: too many nests or wrong notation found where listing by $sp"
	  		    System.exit(-1)
	  		    return ""
	  		}
		
	  		val p = new Regex(regex, "before","elements","element","following")
	  		val m = p findFirstMatchIn(doc)
	  		if(m != None){
	  			var bef = ""
	  			var fol = ""

	  			if(m.get.group("before") != None){bef = m.get.group("before")}else{bef = ""}
	  			if(m.get.group("following") != None){fol = m.get.group("following")}else{fol = ""}
				val s = m.get.group("elements")
				log info s			
				
				var str = ""
				var i = indent
				var list = List.empty[Tuple3[String,Int,String]]
				for(elem <- s"""((\\s*?)\\$sign\\s(.+?)\\\\,)+?""".r.findAllMatchIn(s)){
				  list = (elem.group(1),elem.group(2).size,elem.group(3))::list
				}
				var indents = 0
				for(elem <- list.reverse){
				  if(elem._2 != indents){indents = elem._2}
				  if(indents == i){
					log info elem._1 + ":" + indents + ":" + elem._3
				    str += s"<$TAG>" + elem._3 + s"</$TAG>\\,"
				    log info "^^^" + elem._1
				    tree.add(new TreeNode[String](str))
				    //_surroundByUlListTAG(elem.group("following"),regex,TAG,style.tail,i)
				  }else if(indents > i){
					str += _surroundByListTAG(elem._1,regex,TAG,style.tail,indents)
					log info "%%%" + str
					tree.add(new TreeNode[String](str))
				  }else if(indents < i){
				    str += _surroundByListTAG(elem._1,regex,TAG,style,indents)
					log info "&&&" + str
					tree.add(new TreeNode[String](str))
				  }
				  i = indents
				}
				
				val ST:String = style.head
				log info "---->"  
				for(node <- tree) log info node
				return _surroundByListTAG(bef,regex,TAG,style,0) +
						s"""<$sp style=\"list-style-type:$ST\">""" + str + s"""</$sp>\\,""" +
						_surroundByListTAG(fol,regex,TAG,style,0)						

			}
	  		doc
		  }
	  	_surroundByListTAG(doc,regex,"li",styles,0)
	}
	
	private def surroundByHeadTAG(doc:String, regex:String, TAG:String):String = {
	  	if(doc == ""){return doc}
	  	
	  	log debug "--> " + doc
	  	val p = new Regex(regex, "before","headSize","inTAG","following")
		val m = p findFirstMatchIn(doc)
		if(m != None){
	      var bef = ""
	      var fol = ""

		  if(m.get.group("before") != None){bef = m.get.group("before")}else{bef = ""}
	      if(m.get.group("following") != None){fol = m.get.group("following")}else{fol = ""}
		  val headSize = m.get.group("headSize").size
		  val headTAG = TAG + headSize
		  return surroundByHeadTAG(bef,regex,TAG) +
				  s"<${headTAG}>" + m.get.group("inTAG") + s"</${headTAG}>\\," +
				  surroundByHeadTAG(fol,regex,TAG)
		}
	  doc
	}
	
	private def surroundByAbstructTAG(doc:String, regex:String, TAG:String):String = {
	  if(doc == ""){return doc}
	  log debug doc
	  val p = new Regex(regex,"before","inTAG","following")
	  val m = p findFirstMatchIn(doc)
	  if(m != None){
	      var bef = ""
	      var fol = ""
		  if(m.get.group("before") != None){bef = m.get.group("before")}else{bef = ""}
	      if(m.get.group("following") != None){fol = m.get.group("following")}else{fol = ""}
		  return surroundByAbstructTAG(bef,regex,TAG) + 
				  s"<${TAG}>" + m.get.group("inTAG") + s"</${TAG}>" +
	    		  surroundByAbstructTAG(fol,regex,TAG)
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