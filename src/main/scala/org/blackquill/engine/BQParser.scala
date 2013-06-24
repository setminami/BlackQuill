package org.blackquill.engine

// BlackQuill Copyright (C) 2013 set.minami<set.minami@gmail.com>
// License MIT see also LISENCE.txt
// BQParser.

import org.apache.commons.logging._
import scala.collection.immutable.List
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.HashMap
import scala.collection.mutable.Set
import scala.collection.mutable.Stack
import scala.collection.mutable.ListMap
import scala.collection.SortedSet
import scala.util.matching.Regex
import scala.util.control.Breaks.{break,breakable}
import scala.xml._

import org.blackquill.engine._
import org.blackquill.io.FileIO


class BQParser {
	private val log:Log = LogFactory.getLog(classOf[BQParser])

	private var urlDefMap = new HashMap[String,Tuple5[String,String,String,String,String]]

	private val Syntax = LinkedHashMap(
	//STRONG
	"^(.*?)`(.*)" -> ("code",surroundByCodeTAG _),
	"""^(.*?)\\,\\,((?:\|?.+?\|?)+?)\\,((?:\|?:?\-{3,}:?\|?)+?)\\,((?:\|?.+?\|?\\,)+?)\\,(.*)$$"""
	-> ("table",surroundTableTAG _),
	"^(.*)<([\\w\\d\\.\\-\\_\\+]+?)@([\\w\\d\\.\\-\\_\\+]+?)>(.*)" -> ("a", autoMailLink _),
	"^(.*)<((?:https?|ftp):\\/\\/[\\w\\d\\.\\/]+?)>(.*)$$" -> ("a",autoURLLink _),
	"^(.*)!\\[(.+?)\\]\\[(.*?)\\](?:\\{(.+?)\\})?(.*)$$" -> ("img",referenceExpander _),
	"^(.*)\\[(.+?)\\]\\[(.*?)\\](?:\\{(.+?)\\})?(.*)$$" -> ("a",referenceExpander _),
	"^(.*?)!\\[(.*?)\\]\\((.+?)\\x20*?(?:\"(.+?)\")?(?:\\x20+?(\\d+?%?)?x(\\d+?%?)?)?\\)(?:\\{(.+?)\\})?(.*)$$"
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
//	"^(.*?)([^(?:\\\\,\\\\,).]+)(\\\\,\\,.*?)?$$" -> ("p",surroundByGeneralTAG _)
	//WEAK
	//"^(.*?)(\\\\,.+?\\\\,)(.*?)$$" -> ("p",surroundByAbstructTAG _)
	)

	private def surroundTableTAG(doc:String, regex:String, TAG:String):String = {
	  	def _normalize(text:String):String = {
	  	  var retStr = text
	  	  if(retStr.startsWith("|")){
	  	    retStr = retStr.tail.toString
	  	  }
	  	  if(retStr.endsWith("|")){
	  	    retStr = retStr.init.toString
	  	  }
	  	  return retStr
	  	}
	  	
	  	def _getAlign(alignList:List[String],i:Int):String = {
	  	  if(i >= alignList.size){""}else{alignList(i)}
	  	}

		if(doc == ""){return ""}

		log debug "***" + doc
		val p = new Regex(regex, "before","headSeq","separatorSeq","bodySeq","following")
		val m = p findFirstMatchIn(doc)

		if(m != None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")
			var head = m.get.group("headSeq")
			val sep = m.get.group("separatorSeq")
			val body = m.get.group("bodySeq")

			if(Option(sep) != None){
				val pSep = """((?:\|)?(:?-{3,}?:?)(?:\|)?)+?""".r
				val mSep = pSep.findAllMatchIn(sep)

				var tableList = List[List[String]]()
				var tmpList = List[String]()
				for(mS <- mSep){
					val align = mS.group(2)
					if(align.startsWith(":") && align.endsWith(":")){
						tmpList ::= """align=\"center\" """
					}else if(align.startsWith(":")){
						tmpList ::= """align=\"left\" """
					}else if(align.endsWith(":")){
						tmpList ::= """align=\"right\" """
					}else{
					  tmpList ::= ""
					}
				}
				val alignList = tmpList.reverse
				head = _normalize(head)
				log info head
				val heads = for((h,i) <- head.split("\\|").zipWithIndex)yield(s"""<th ${_getAlign(alignList,i)}>$h</th>\\,""")
				val headList = heads.toList
				if(headList.size != alignList.size){
					log error "Table header is wrong.:" + headList
					exit(-1)
				}


				log debug headList
				log debug alignList


				val pTBody = """((((\|)?(.*?)(\|)?)+?)\\,?)+?""".r
				val mTBSeq = pTBody.findAllMatchIn(body)
				var bodyList = List[String]()
				tmpList = List.empty
				for((mTBS,i) <- mTBSeq.zipWithIndex){
					val row = _normalize(mTBS.group(2)).split("\\|")
					val body =  for((c,j) <- row.zipWithIndex)yield(s"""<td ${alignList(j)}>$c</td>\\,""")
					bodyList ::= "<tr>\\\\," + body.mkString("") + "</tr>\\\\,"
				}

				bodyList = bodyList.reverse
				log debug bodyList
				return surroundTableTAG(bef, regex, TAG) +
					"\\\\,<table><thead>\\\\," + s"<tr>${headList.mkString("")}</tr></thead>\\\\," +
					s"<tbody>${bodyList.mkString("")}</tbody></table>\\\\," +
					surroundTableTAG(fol, regex, TAG)

			}

		}
		doc
	}

	private def autoMailLink(doc:String, regex:String, TAG:String):String = {
		if(doc == ""){return ""}

		val p = new Regex(regex, "before","inTAG","domain","following")
		val m = p findFirstMatchIn(doc)

		if(m!= None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")
			val mail = m.get.group("inTAG")
			val domain = m.get.group("domain")

			return autoMailLink(bef, regex, TAG) +
				s"""<script type=\"text/javascript\">\\,document.write('<$TAG href=\\"mailto:$mail')\\,document.write(\"@\")\\,document.write(\"$domain\\">MailMe!</$TAG>\") </script>""" +
				autoMailLink(fol, regex, TAG)
		}
		doc
	}
	private def autoURLLink(doc:String, regex:String, TAG:String):String = {
		if(doc == ""){return ""}

		val p = new Regex(regex, "before","inTAG","following")
		val m = p findFirstMatchIn(doc)

		if(m!= None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")
			val url = m.get.group("inTAG")

			return autoURLLink(bef, regex, TAG) + s"""<$TAG href=\"$url\">$url</$TAG> """ +
				autoURLLink(fol, regex, TAG)
		}
		doc
	}
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

	private def referenceExpander(doc:String, regex:String, TAG:String):String = {
		val m = regex.r.findFirstMatchIn(doc)
		def expandAttribute(value:String):String = value match{
			case "" => return ""
			case _ => return value
		}

		if(m != None){
			var key = ""
			if(m.get.group(3) != ""){
			  key = m.get.group(3)
			}else{
			  key = m.get.group(2).toLowerCase()
			}

			if(urlDefMap.contains(key)){
				val tup5 = urlDefMap(key)
				var css = ""
				if(m.get.group(4) == null){
					css = expandAttribute(tup5._5)
				}else{
					css = decideClassOrStyle(doc,m.get.group(4))
				}
				TAG match {
					case "a" =>
						return referenceExpander(m.get.group(1), regex, TAG) +
						s"""<$TAG href=\"${tup5._1}\" ${expandAttribute(tup5._2)}$css>""" +
						m.get.group(2) + s"""</$TAG>""" + referenceExpander(m.get.group(5), regex, TAG)
					case "img" =>
						return referenceExpander(m.get.group(1), regex, TAG) +
						s"""<$TAG src=\"${tup5._1}\" alt=\"${m.get.group(2)}\" ${expandAttribute(tup5._2)}${expandAttribute(tup5._3)}${expandAttribute(tup5._4)} $css>""" +
						referenceExpander(m.get.group(5), regex, TAG)
					case _ =>
						log error "Unknown Expand TAG from Reference"
						exit(-1)
				}
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
			if(text == ""){return text}

		    log info "doc ==>" + text
		    val p = new Regex("""^(.*?)?(((\[([\w\d\.\_\+\-\:\/)]+?)\]:([\w\d\.\_\+\-\:\/]+?)(?:\s+\"(.+?)\")?(?:\s+(\d+%?x\d+%?))?(?:\s+\{(.+?)\})?)\s*\\,)+?)(?:\\,|\z)(.*)?$$""",
				"before","seq","elem1","elem2","landMark","link","Title","Res","Css", "following")
		    val m = p findFirstMatchIn(text)

			if(m != None){
				if(m.get.group("before") != None){bef = m.get.group("before")}
				if(m.get.group("following") != None){fol = m.get.group("following")}

				log debug "bef=>" + bef
				log info "seq=>" + m.get.group("seq")
				log debug "fol=>" + fol
				if(m.get.group("seq") != None){
					val seq = m.get.group("seq")
					val mat =
					"""\[([\w\d\.\_\+\-\:\/]+?)\]:([\w\d\.\_\+\-\:\/]+)(\s+\"(.+?)\")?(?:\s+(\d+%?x\d+%?))?(?:\s+\{(.+?)\})?(?:\s*\\,)?""".r.findAllMatchIn(seq)
					for(e <- mat){
						val link = e.group(2)
						log info ">>" + link
						val landMark = e.group(1)
						log info ">>>" + landMark
						var title = ""
						if(e.group(4) != null){title = s"""title=\"${e.group(4)}\" """}
						var resX = ""
						var resY = ""
						if(Option(e.group(5)) != None){
							val matResolution = """(\d+%?)x(\d+%?)""".r.findFirstMatchIn(e.group(5))
							resX = getResolutionX(matResolution.get.group(1))
							resY = getResolutionY(matResolution.get.group(2))
						}
						var css = ""
						if(e.group(6) != null){
							css = decideClassOrStyle(text,e.group(6))
						}
						urlDefMap += (landMark.toLowerCase->(link,title,resX,resY,css))
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
	  if(doc == ""||Option(doc) == None){return ""}
	  log info doc
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

		md = backslashEscape(md)
		md = paragraphize(md)
	  	log info urlDefMap
		val header = constructHEADER(markdown)
		s"${docType}\n${header}\n<${htmlTAG}>\n<${bodyTAG}>\n${md.replaceAll("\\\\,","\n")}\n</${bodyTAG}>\n</${htmlTAG}>"
	}

	private def paragraphize(doc:String):String = {
		val delimiter = """\,"""
		def f(text:String):String = {
			text + delimiter
		}
		val BlockElements = new HTMLMap().BLOCKTags
		var isBlock = false
		var isOneLineBlock = false
		var text = ""
		var pg = ""

		for(l <- doc.split("\\" + delimiter)){
			isOneLineBlock = false
			log debug l
			breakable{
				for(e <- BlockElements){
				  log debug e
					if(l.startsWith("<" + e) &&  l.endsWith("</" + e + ">")){
						isOneLineBlock = true;break;
					}else if(l.startsWith("<" + e)){
						isBlock = true;break;
					}else if(l.endsWith("</" + e + ">")){
						isBlock = false;isOneLineBlock = true;break;
					}
				}
			}
			log debug ">>>>" + l + "::" + isBlock + "|" + isOneLineBlock
			if(isBlock | isOneLineBlock){
				text += l + delimiter
			}else{
				if(l != ""){text += "<p>" + l + "</p>" + delimiter}
			}
		}
		text
		//var text = "<p>" + doc.replaceAll("\\\\,\\\\,","</p>\\\\,\\\\,<p>") + "</p>"
		//text.replaceAll("<p></p>","")
	}

	private def backslashEscape(doc:String):String = {
		val escapeCharSet = Set("\\","`","*","_","{","}","[","]","(",")","#","+","-","!",":","|")
		var bef = ""
		for(e <- doc){
			if(bef.size > 2 && escapeCharSet.contains(e.toString) && bef.reverse.head.toString == "\\"){
				bef = bef.init + e
			}else{
				bef += e
			}
		}
		return bef
	}

}