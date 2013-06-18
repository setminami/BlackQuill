package org.blackquill.engine

// BlackQuill Copyright (C) 2013 set.minami<set.minami@gmail.com>
// License MIT see also LISENCE.txt
// BQParser.

import org.apache.commons.logging._
import scala.collection.immutable.List
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.HashMap
import scala.collection.mutable.Stack
import scala.collection.mutable.ListMap
import scala.collection.SortedSet
import scala.util.matching.Regex
import scala.xml._

import org.blackquill.engine._
import org.blackquill.io.FileIO


class BQParser {
	private val log:Log = LogFactory.getLog(classOf[BQParser])

	private var urlDefMap = new HashMap[String,Tuple2[String,String]]

	private val Syntax = LinkedHashMap(
	//STRONG
	"^(.*?)`(.*)" -> ("code",surroundByCodeTAG _),
	"^(.*)\\[(.+?)\\]\\[(.*?)\\](.*)$$" -> ("a",expandUrlDefinitions _),
	"^(.*?)!\\[(.*?)\\]\\((.+?)\\x20*?(?:\"(.+?)\")?(?:\\x20+?(\\d+?%?)?x(\\d+?%?)?)?\\)(?:\\{(.+?)\\}){0,1}(.*)$$"
	-> ("img", putImgTAG _),
	"^(.*?)\\[(.*?)\\]\\((.+?)\\x20*?(?:\"(.+?)\")?\\)(?:\\{(.+?)\\})?(.*?)$$" -> ("a", surroundaHrefTAG _),
	"^(.*?\\\\,)(((?:\\x20{4,}|\\t+)(.*?\\\\,))+)(.*?)$$" -> ("code",surroundByPreCodeTAG _),
	"^(.*?\\\\,)((>.*(?:\\\\,))+?)(.*?)$$" -> ("blockquote",surroundByBlockquoteTAG _),
	"^(.*?)(((?:\\x20{4,}|\\t+)\\d+?\\.\\x20.+?\\\\,)+)(.*?)$$" -> ("ol",surroundByListTAG _),
	"^(.*?)(((?:\\x20{4,}|\\t+)(?:\\*|\\+|\\-)\\x20.+?\\\\,)+)(.*?)$$" -> ("ul",surroundByListTAG _),
	"^(.*?)(#{1,6})\\x20(.+?)(\\x20#{1,6}?)??\\\\,(.*?)$$" -> ("h",surroundByHeadTAG _),
	"^(.*\\\\,)(.*?)\\\\,(\\-+|=+)\\x20*\\\\,(.*?)$$" -> ("h",surroundByHeadTAGUnderlineStyle _),
	"^(.*\\\\,)((?:\\-|\\*){3,}|(?:(?:\\-|\\*)\\x20){3,})(.*?)$$" -> ("hr",putHrTAG _),
	"^(.*?)\\*\\*(.+?)\\*\\*(.*?)$$" -> ("strong",surroundByGeneralTAG _),
	"^(.*?)\\*(.+?)\\*(.*?)$$" -> ("em",surroundByGeneralTAG _)
	//WEAK
	//"^(.*?)(\\\\,.+?\\\\,)(.*?)$$" -> ("p",surroundByAbstructTAG _)
	)

	private def putImgTAG(doc:String, regex:String, TAG:String):String = {
		if(doc == ""){return ""}

		val p = new Regex(regex,"before","alt","url","title","resX","resY","css","following")
		val m = p findFirstMatchIn(doc)

		if(m != None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")
			val alt = m.get.group("alt")
			val url = m.get.group("url")
			return putImgTAG(bef,regex,TAG) + s"""<$TAG src=\"$url\" alt=\"$alt\" ${getTitleName(m.get.group("title"))}""" +
				s"""${getResolutionX(m.get.group("resX"))}${getResolutionY(m.get.group("resY"))}${decideClassOrStyle(doc,m.get.group("css"))}>""" +
				putImgTAG(fol,regex,TAG)
		}
		doc
	}

	private def getResolutionX(resX:String):String = Option(resX) match{
		case None => return ""
		case Some(x) => return s" width=$resX "
		case _ =>
			log error "unknown parameter has found." + resX
			exit(-1)
	}

	private def getResolutionY(resY:String):String = Option(resY) match{
		case None => return ""
		case Some(y) => return s" height=$resY "
		case _ =>
			log error "unknown parameter has found." + resY
			exit(-1)
	}
	private def searchCSSClassName(doc:String,cssClass:String):Boolean = {
		val p = """(?i)<link.*?type=\"text\/css\".*?href=\"(.*?)\".*?>""".r
		val m = p findFirstMatchIn(doc)

		if(m != None){
			val fileName = m.get.group(1)
			val CSSHandler = FileIO
			val CSS = CSSHandler openCSSFile(fileName) mkString("")
			log debug CSS
			for(line <- CSS.split("""\/\*.*?\*\/""")){
				log debug "***" + line
				if(line.contains(cssClass + " ")){return true}
			}
		}
		false
	}

	private def surroundByCodeTAG(doc:String, regex:String, TAG:String):String = {
	    def _surroundByCodeTAG(doc:String,regex:String,innerSign:String,TAG:String):String = {
	    	if(doc.contains(innerSign)){
	    		val p = regex.r
	    		val m = p.findFirstMatchIn(doc)

	    		if(m != None){
	    			val bef = m.get.group(1)
	    			var fol = m.get.group(2)
		    		var sign = "`"
		    		log debug "=>" + fol.head + " " + fol
	    			var follow = ""
	    			if((fol.head.toString == sign)&&(sign != "``")){
	    				sign = "``"
	    				follow = fol.tail
	    			}else{
	    				follow = fol
	    			}
	    			log debug "**>" + follow
	    			log debug "==>" + sign
	    			val p2 = s"""^([^(?:\\,)]+?)$sign(.*)$$""".r
	    			val m2 = p2.findFirstMatchIn(follow)

	    			if(m2 != None){
	    				log debug ">>>>" + m2.get.group(1)
	    				return _surroundByCodeTAG(bef, regex, "`", TAG) + s"<$TAG>" + m2.get.group(1) + s"</$TAG>" +
	    					_surroundByCodeTAG(m2.get.group(2), regex, "`", TAG)
	    			}else{
	    				if(fol.startsWith("```")){
	    					return _surroundByCodeTAG(bef, regex, "`", TAG) + s"<$TAG></$TAG>" +
	    						_surroundByCodeTAG(follow.drop(2), regex, "`", TAG)
	    			  	}else if(fol.startsWith("`")){
	    					return _surroundByCodeTAG(bef, regex, "`", TAG) + s"<$TAG></$TAG>" +
		    					_surroundByCodeTAG(follow.drop(0), regex, "`", TAG)
	    			  	}else{
							log warn s"$sign CodeBlock is wrong."
			    			return ""
			    		}
	    			}
	    		}else{return doc}
	    	}else{return doc}
	    }
    _surroundByCodeTAG(doc,regex,"`",TAG)
	}

	private def expandUrlDefinitions(doc:String, regex:String, TAG:String):String = {
		val m = regex.r.findFirstMatchIn(doc)
		if(m != None){
			var key = ""
			if(m.get.group(3) != ""){
			  key = m.get.group(3)
			}else{
			  key = m.get.group(2).toLowerCase()
			}

			if(urlDefMap.contains(key)){
				return expandUrlDefinitions(m.get.group(1), regex, TAG) +
						s"""<$TAG href=\"${urlDefMap(key)._1}\" title=\"${urlDefMap(key)._2}\">""" +
						m.get.group(2) + s"""</$TAG>""" + expandUrlDefinitions(m.get.group(4), regex, TAG)
			}else{
			  log warn "Link definition was not found : " + key
			  doc
			}
		}else{
		  doc
		}

	}

	private def urlDefinitions(doc:String):String = {
		def _urlDefinitions(text:String):String = {
		    var bef = ""
 		    var fol = ""

		    log debug "doc ==>" + text
		    val p = new Regex("""^(.*?){0,1}(((\[([\w\d\.\,\-]+?)\]:([\w\d:\/\.]+?)(\s+\"(.+?)\"){0,1})\\,)+?)(?:\\,|\z)(.*){0,1}$$""",
				"before","seq","elem1","elem2","landMark","link","test","Title", "following")
		    val m = p findFirstMatchIn(text)
			if(text == ""){return text}
			if(m != None){
				if(m.get.group("before") != None){bef = m.get.group("before")}
				if(m.get.group("following") != None){fol = m.get.group("following")}

				log debug "bef=>" + bef
				log debug "seq=>" + m.get.group("seq")
				log debug "fol=>" + fol
				if(m.get.group("seq") != None){
					val seq = m.get.group("seq")
					val mat = """\[(.+?)\]:([\w\d:\/\.]+)(\s+\"(.+)\"){0,1}(?:\\,){0,1}""".r.findAllMatchIn(seq)
					for(e <- mat){
						val link = e.group(2)
						log debug ">>" + link
						val landMark = e.group(1)
						log debug ">>>" + landMark
						var title = ""
						if(e.group(4) != null){title = e.group(4)}

						urlDefMap += (landMark.toLowerCase->(link,title))
					}
				}
				_urlDefinitions(bef) + _urlDefinitions(fol)
			}else{
			  log debug "m was None!"
			  text}

		}
		_urlDefinitions(doc)
	}

	private def decideClassOrStyle(doc:String,className:String):String = {
		if(className == "" || className == null){
			return ""
		}

		if(!searchCSSClassName(doc,className)){
			if(!className.contains(":")){
				log warn s"[CSS] $className Class Name Not Found."
				return "class=\"" + className +"\""
			}
			return "style=\"" + className + "\""
		}else{
			return "class=\"" + className +"\""
		}
	}

	private def getTitleName(title:String):String = {
		if(title == ""| title == null){
			return ""
		}

		return s"""title=\"$title\" """
	}

	private def surroundaHrefTAG(doc:String,regex:String,TAG:String):String = {
		val p = new Regex(regex,"before","inTag","link","title","css","following")
		val m = p findFirstMatchIn(doc)

		var bef = ""
		var fol = ""
		if(m != None){
			if(m.get.group("before") != None){
			  bef = m.get.group("before")
			}
			if(m.get.group("following") != None){fol = m.get.group("following")}

			val link = m.get.group("link")
			val label = m.get.group("inTag")
			return surroundaHrefTAG(bef, regex, TAG) +
					s"""<$TAG href=\"$link\" ${getTitleName(m.get.group("title"))}""" +
					s"""${decideClassOrStyle(doc,m.get.group("css"))}>$label</$TAG>""" +
					surroundaHrefTAG(fol,regex,TAG)
		}
		doc
	}

	private def putHrTAG(doc:String, regex:String, TAG:String):String = {
	  	val p = new Regex(regex,"before","line","following")
	  	val m = p findFirstMatchIn(doc)

	  	log debug doc

	  	var bef = ""
	  	var fol = ""
	  	if(m != None){
	  		if(m.get.group("before") != None){
	  		bef = m.get.group("before")
	  		}
	  		if(m.get.group("following") != None){fol = m.get.group("following")}

	  		return putHrTAG(bef,regex,TAG) + s"<$TAG />" + putHrTAG(fol,regex,TAG)
	  	}
	  doc
	}

	private def surroundByPreCodeTAG(doc:String, regex:String, TAG:String):String = {
		val p = new Regex(regex, "before","seq","inTAG","midInTag","following")
		val m = p findFirstMatchIn(doc)
		log debug "[" + doc + "]"

		var bef = ""
		var fol = ""
		var contentStr = ""
		if(m != None){
		  if(m.get.group("before") != None){
		    bef = m.get.group("before")
		  }
		  if(m.get.group("following") != None){fol = m.get.group("following")}

		  log debug "^^^" + m.get.group("seq")
		  val mat = """((?:\x20{4,}|\t+)(.*?\\,))+?""".r.findAllMatchIn(m.get.group("seq"))
		  for(elem <- mat){
		    contentStr += elem.group(2)
		    log debug "***" + contentStr
		  }

		  log debug contentStr
		  return surroundByPreCodeTAG(bef,regex,TAG) +
				  s"<pre><$TAG>" +
				  contentStr.replaceAll("&","&amp;").replaceAll("<","&lt;").replaceAll(">","&gt;") +
				  s"</$TAG></pre>\\," +
				  surroundByPreCodeTAG(fol, regex, TAG)
		}

	  doc
	}

	private def surroundByBlockquoteTAG(doc:String, regex:String, TAG:String):String = {
	  val p = new Regex(regex, "before","inTAG","midInTag","following")
	  val m = p findFirstMatchIn(doc)
	  log debug "[" + doc + "]"

	  var bef = ""
	  var fol = ""
	  var contentStr = ""
	  if(m != None){
	    var mid = m.get.group("inTAG")
	    log debug "***-->" + mid

	    if(m.get.group("before") != None){
	      bef = m.get.group("before")
	      if(bef.startsWith(">")){
	        mid = bef + mid
	        bef = ""
	      }
	    }else{bef = ""}
  		if(m.get.group("following") != None){fol = m.get.group("following")}else{fol = ""}

  		log debug "=>" + mid
  		var following = ""
		if(mid != null){
		  val mat = """(.+?\\,)+?""".r.findAllMatchIn(mid)

		  var inCurrentBQ = true
		  for(mt <- mat){
		    if(!mt.group(1).startsWith(">")||mt.group(1) == "\\\\,"){
		    	inCurrentBQ = false
		    }
		    if(inCurrentBQ){
		      val m = """.*?<br />\\,$$""".r.findFirstMatchIn(mt.group(1))
		      var break = "\\\\,"
		      if(m == None){break = "<br />\\\\,"}
		      contentStr += mt.group(1).tail.replaceAll("\\\\,", break)
		    }else{
		      log debug "(" + mt.group(1) + ")"
		      following += mt.group(1)
		    }
		    log debug "^^^" + mt
		  }
		}

  		following += fol
  		log debug "bef=" + bef + " mid=" + contentStr + " fol="  + following
		log debug "-->" + contentStr
		return surroundByBlockquoteTAG(bef, regex, TAG) +
				s"<$TAG>\\," + surroundByBlockquoteTAG(contentStr, regex, TAG) + s"</$TAG>\\," +
				surroundByBlockquoteTAG(following, regex, TAG)
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

		log debug ":::" + s
		var docList = List[String]()
		for(elem <- s"""(\\x20+?$sign\\x20.+?\\\\,)+?""".r.findAllMatchIn(s)){
			docList = elem.group(1)::docList
		}



	  	def _surroundByListTAG(doc:List[String],TAG:String,indent:Int):TreeNode[String] = {
	  		var tree = new TreeNode[String]("")
	  	    if(doc.isEmpty){return tree}

	  		log debug "====>" + doc
	  		tree.add(new TreeNode("<" + sp + s""" style=\"list-style-type:${styles(indent)}\">"""))
			var i = indent
			var list = List.empty[Tuple3[String,Int,String]]
	  		for(elem <- doc){
	  		  val m = s"""((\\x20+?)$sign\\x20(.+?)\\\\,)""".r.findFirstMatchIn(elem)
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

	  	log debug "->" + docList
	  	val r1 = s"""(\\x20*)${sign}.*?\\\\,""".r
	  	val wS1 = r1.findFirstMatchIn(s)
	  	var str = ""
	  	val r2 = s"""(\\x20*)${sign}.*(\\\\,<br />.*?</blockquote>\\\\,)""".r
	  	val wS2 = r2.findFirstMatchIn(s)

	  	var wS:Option[Regex.Match] = null
	  	if(wS2 != None){wS = wS2}else if(wS1 != None){wS = wS1}
	  	if(wS != None){
	  		for(e <- _surroundByListTAG(docList.reverse,"li",wS.get.group(1).size)){
	  		  str += e.toString()
	  		}
	  	  if(wS == wS2){str += wS.get.group(2)}

	  	  log debug "!---->" + str
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
		    val m2 = """^\x20(#+?)$$""".r("wantSize").findFirstMatchIn(endHead)
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

	  val p = new Regex("""(?i)#{1,6}\x20(.+?)\\,""")
	  val m = p findFirstMatchIn(doc)
	  if(m != None){title = m.get.group(1)}

	  s"<${headTAG}>\n<${titleTAG}>${title}</${titleTAG}>\n</${headTAG}>"
	}

	def preProcessors(doc:String) :String = {
	 val text = urlDefinitions(doc)
	 text
	}

	def toHTML(markdown:String):String = {
	  	val docType = "<!DOCTYPE html>"
	  	val bodyTAG = "body"
	  	val htmlTAG = "html"
	  	var md = preProcessors(markdown + "\\,")

		for(k <- Syntax keys){
		 md = Syntax(k)._2(md, k, Syntax(k)._1)
		}
	  	log debug urlDefMap
		val header = constructHEADER(markdown)
		s"${docType}\n${header}\n<${htmlTAG}>\n<${bodyTAG}>\n${md.replaceAll("\\\\,","\n")}\n</${bodyTAG}>\n</${htmlTAG}>"
	}


}