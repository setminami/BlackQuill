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

import java.io.ByteArrayInputStream

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.DocumentBuilder

//import org.w3c.dom
import org.blackquill.engine._
import org.blackquill.main._
import org.blackquill.io.FileIO
import org.blackquill.breadboard.Latexconverter


class BQParser {
	private val log:Log = LogFactory.getLog(classOf[BQParser])

	private var urlDefMap = new HashMap[String,Tuple5[String,String,String,String,String]]
	private var footnoteMap = new LinkedHashMap[String,Tuple2[String,String]]
	private var headerMap = List[Tuple4[Int,Int,String,String]]()

	private var nRange = (-1,-1)
	private var nStack = Stack[Tuple3[Int,Int,String]]()

	private val texSignStart = """\\begin\{TeX\}"""
	private val texSignEnd = """\\end\{TeX\}"""

	private val Syntax = LinkedHashMap(
	//Early
	"""^(.*?)(%{1,6})\x20(.*?)(\\,.*?)$$""" -> ("h", autoNumberingHeader _),
	s"""^(.*?)$texSignStart(.*?)$texSignEnd(.*?)$$""" -> ("", laTeXConvert _),
	"""^(.*?)(([^(?:\\,)]+?\\,(:(.+?)\\,)+)+)(.*?)$$""" -> ("dl", wordDefinition _),
	"""(.*)\\,~{3,}(?:\{(.+?)\})?(\\,.+\\,)~{3,}\\,(.*)""" -> ("code", fencedCode _),
	"""^(.*?)(?:\[(.+?)\](?:\{(.+?)\})?\\,)?((\|.+?)+?\|)\\,((\|:?\-{3,}:?)+?\|)\\,(((\|.+?\|?)+?\\,)+?)\\,(.*?)$$"""
	-> ("table",surroundTableTAG _),
	"^(.*?)`(.*)" -> ("code",surroundByCodeTAG _),
	"^(.*)<([\\w\\d\\.\\-\\_\\+]+?)@([\\w\\d\\.\\-\\_\\+]+?)>(.*)" -> ("a", autoMailLink _),
	"^(.*)<((?:https?|ftp):\\/\\/[\\w\\d\\.\\/]+?)>(.*)$$" -> ("a",autoURLLink _),
	"""^(.*)\[\^(.+?)\](.*)$$""" -> ("sup",insertFootnote _),
	"""(.*)\*\[(.*?)\]\{(#?[\w\d]+?)?(/#?[\w\d]+?)?(\(\+?(.*?)?(\|\+?(.*?))?\))?(\[(.*?)\])?\}(.*)"""
	-> ("span",colorPen _),
	"^(.*)!\\[(.+?)\\]\\[(.*?)\\](?:\\{(.+?)\\})?(.*)$$" -> ("img",referenceExpander _),
	"^(.*)\\[(.+?)\\]\\[(.*?)\\](?:\\{(.+?)\\})?(.*)$$" -> ("a",referenceExpander _),
	"^(.*?)!\\[(.*?)\\]\\((.+?)\\x20*?(?:\"(.+?)\")?(?:\\x20+?(\\d+?%?)?x(\\d+?%?)?)?\\)(?:\\{(.+?)\\})?(.*)$$"
	-> ("img", putImgTAG _),
	"^(.*?)\\[(.*?)\\]\\((.+?)\\x20*?(?:\"(.+?)\")?\\)(?:\\{(.+?)\\})?(.*?)$$" -> ("a", surroundaHrefTAG _),
	"^(.*?\\\\,)(((?:\\x20{4,}|\\t+)(.*?\\\\,))+)(.*?)$$" -> ("code",surroundByPreCodeTAG _),
	"^(.*?\\\\,)((>.*(?:\\\\,))+?)(.*?)$$" -> ("blockquote",surroundByBlockquoteTAG _),
	"^(.*?)(((?:\\x20{4,}|\\t+)\\d+?\\.\\x20.+?\\\\,)+)(.*?)$$" -> ("ol",surroundByListTAG _),
	"^(.*?)(((?:\\x20{4,}|\\t+)(?:\\*|\\+|\\-)\\x20.+?\\\\,)+)(.*?)$$" -> ("ul",surroundByListTAG _),
	"^(.*?)(#{1,6})\\x20(.+?)(\\x20#{1,6}?(?:\\{(.*?)\\}))?\\\\,(.*?)$$" -> ("h",surroundByHeadTAG _),
	"^(.*\\\\,)(.*?)(?:\\{(.+?)\\})?\\\\,(\\-+|=+)\\x20*\\\\,(.*?)$$" -> ("h",surroundByHeadTAGUnderlineStyle _),
	"""^(.*?)(\{toc(:.+?)?\})(.*)$$""" -> ("ul",generateTOC _),
	"^(.*\\\\,)((?:\\-|\\*){3,}|(?:(?:\\-|\\*)\\x20){3,})(.*?)$$" -> ("hr",putHrTAG _),
	"^(.*?)\\*\\*(.+?)\\*\\*(.*?)$$" -> ("strong",surroundByGeneralTAG _),
	"^(.*?)\\*(.+?)\\*(.*?)$$" -> ("em",surroundByGeneralTAG _),
	"""^(.*)\|\-:b\s*=\s*(\d+?)\s*(\w*?)\s*(#?[\w\d]+?)\sw\s*=\s*(\d+?)\srad\s*=\s*(\d+?)\-+?\|(.*?)$$""" -> ("div",fencedBox _),
	"""^(.*)\|\-:\{(.*?)\}\|(.*?)""" -> ("div",fencedBoxByClass _)
	//late
	)

	private def fencedBoxByClass(doc:String, regex:String, TAG:String):String = {
		val p = new Regex(regex,"before","class","following")
		val m = p findFirstMatchIn(doc)

		if(m != None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")

			val claz = m.get.group("class")
			if(searchCSSClassName(doc,claz)){
				return fencedBoxByClass(bef,regex,TAG) + s"""<$TAG class="$claz"> """ + _searchEndMark(fol,regex,TAG)
			}else{
				log error s"Class Name:$claz Not found"
				exit(1)
			}
		}
		doc
	}

	private def _searchEndMark(doc:String,regex:String,TAG:String):String = {
		val p2 = """^(.*?)\|_{3,}\|(.*?)$$""".r
		val m2 = p2 findFirstMatchIn(doc)

		if(m2 != None){
			val p3 = """(.*)\|\-:.*?\\,(.*)""".r
			val m3 = p3 findFirstMatchIn(m2.get.group(1))
			if(m3 != None){
				return fencedBox(m2.get.group(2),regex,TAG)
			}else{
				return m2.get.group(1) + "</div>" + _searchEndMark(m2.get.group(2),regex,TAG)
			}
		}//else{
			//log error "fenced box ERROR: not Found break Mark"
			//exit(-1)
		//}
		doc
	}

	private def fencedBox(doc:String, regex:String, TAG:String):String = {

		val p = new Regex(regex,"before","border","style","color","width","rad","following")
		val m = p findFirstMatchIn(doc)

		if(m != None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")

			val borderW = if(m.get.group("border") != None){
				m.get.group("border") + "px "
			}else{"1"}
			val borderStyle = if(m.get.group("style") != None){
				m.get.group("style")
			}else{"solid"}
			val borderColor = if(m.get.group("color") != None){
				" " + m.get.group("color")
			}else{"black"}
			val boxW = if(m.get.group("width") != None){
				m.get.group("width") + "px"
			}else{
				log error "FENCED BOX WIDTH is not set."
				exit(-1)
			}
			val boxRad = if(m.get.group("rad") != None){
				m.get.group("rad") + "px"
			}else{"10px"}

			val div =
			s"""<$TAG style="border:$borderW$borderStyle$borderColor; width:$boxW;""" +
			s"""border-radius:$boxRad;"> """

			return fencedBox(bef,regex,TAG) + div + _searchEndMark(fol,regex,TAG)
		}
		doc
	}

	private def autoNumberingHeader(doc:String, regex:String, TAG:String):String = {
		def _popTheWastes(i:Int):Stack[Tuple3[Int,Int,String]] = {
			if(nStack.top._1 > i){
				nStack.pop
			}else if(nStack.top._1 == i){
				val top = nStack.pop
				nStack.push(Tuple3[Int,Int,String](top._1,top._2 + 1,"." + top._3))
				return nStack
			}
			return _popTheWastes(i)
		}

		val p = new Regex(regex,"before","hSize","inTAG","following")
		val m = p findFirstMatchIn(doc)

		if(m != None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")
			val inTAG = m.get.group("inTAG")

			val sizeCheck = if(nRange._1 != -1){nRange._1 + m.get.group("hSize").size}else{0}
			val headSize = if(sizeCheck != 0 && sizeCheck - 1 < nRange._2){sizeCheck - 1}else{
				log error "Auto Numbering header FATAL Error. % anotation overflowed. Check nrange value and % sequences again. You can use sequence % - " + "%" * (nRange._2 - nRange._1)
				log info nRange
				exit(-1)
			}

			if(nStack.isEmpty){
				if(nRange._1 != -1 && nRange._2 != -1){
					nStack.push(Tuple3[Int,Int,String](headSize,1,"." + inTAG))
				}else{
					log error "Auto numbering header Error. {nrange..} notation is not set, but found % anotation. You can use sequence % - " + "%" * (nRange._2 - nRange._1)
					exit(-1)
				}
			}else if(nStack.top._1 < headSize){
				nStack.push(Tuple3[Int,Int,String](headSize,1,"." + inTAG))
			}else if(nStack.top._1 > headSize){
				nStack = _popTheWastes(headSize)
			}else if(nStack.top._1 == headSize){
				val top = nStack.pop
				nStack.push(Tuple3[Int,Int,String](top._1,top._2 + 1,top._3))
			}

			val hSize = nStack.top._1
			var number = ""
			for(e <- nStack.toList.reverse){number += e._2 + "."}
			return autoNumberingHeader(bef,regex,TAG) + s"""<$TAG$hSize>$number $inTAG</$TAG$hSize>""" +
			autoNumberingHeader(fol,regex,TAG)
		}
		doc
	}

	private def autoNumberSetting(doc:String):String = {
		val p = """^(.*)(\{nrange(:h?\d?\-h?\d?)?\})(.*)$$""".r
		val m = p findFirstMatchIn(doc)
		if(m != None){
			val ret = m.get.group(1) + m.get.group(4)
			if(Option(m.get.group(3)) != None){
				val p2 = """:(h?(\d)?\-h?(\d)?)""".r
				val m2 = p2 findFirstMatchIn(m.get.group(3))

				if(m2 != None){
					val start = if(Option(m2.get.group(2)) != None){
						m2.get.group(2).toInt
					}else{1}
					val end = if(Option(m2.get.group(3)) != None){
						m2.get.group(3).toInt
					}else{6}
					nRange = (start,end)
					return ret
				}else{
					return ret
				}
			}
		}
		doc
	}

	private def laTeXConvert(doc:String, regex:String, TAG:String):String = {
		if(doc == ""){return ""}
		val p = new Regex(regex, "before","tex","following")
		val m = p findFirstMatchIn(doc)

		if(m != None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")
			val conv = new Latexconverter

			return laTeXConvert(bef,regex,TAG) +
			conv.Convert(m.get.group("tex")) +
			laTeXConvert(fol,regex,TAG)
		}
		doc
	}

	private def colorPen(doc:String, regex:String, TAG:String):String = {
		lazy val fontSize = Map[Int,String](0 -> "medium", 1 -> "larger", 2 -> "large", 3 -> "x-large", 4 -> "xx-large",
														  -1 -> "smaller", -2 -> "small", -3 -> "x-small", -4 -> "xx-small")
		lazy val fontWeight = Map(0 -> "normal", 1 -> "bolder", 2 -> "bold" ,-1 -> "light")
		if(doc == ""){return ""}
		val p = new Regex(regex, "before","content","fcolor","bcolor","fstyle","size","dummy","weight","face","ffamily","following")
		val m = p findFirstMatchIn(doc)

		if(m != None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")

			if(Option(m.get.group("content")) != None){
				var content = m.get.group("content")
				val textDec = if(content.head == content.last){
					  content.head match{
					    case '~' => content = content.tail.init;" text-decoration:overline;"
					    case '-' => content = content.tail.init;" text-decoration:line-through;"
					    case '_' => content = content.tail.init;" text-decoration:underline;"
					    case _ =>
					  }
					}else{""}
				val fgColor = if(Option(m.get.group("fcolor")) != None){" color:" + m.get.group("fcolor") + ";"}else{""}
				val bgColor = if(Option(m.get.group("bcolor")) != None){" background-color:" + m.get.group("bcolor").tail + ";"}else{""}

				val fSize = if(Option(m.get.group("size")) != None){
				  try{
					  log info m.get.group("size")
					  " font-size:" + fontSize(m.get.group("size").toInt) + ";"
				  }catch{ case e:Exception => log info e;" font-size:" + fontSize(0) + ";"}
				}else{""}

				val fWeight = if(Option(m.get.group("weight")) != None){
				  try{
					  " font-weight:" + fontWeight(m.get.group("weight").toInt)
				  }catch{ case e:Exception => log info e ;" font-weight:" + fontWeight(0) + ";"}
				}else{
				  ""
				}

				val fFace = if(Option(m.get.group("ffamily")) != None){
					" font-family:" + m.get.group("ffamily").split(",").mkString("'", "','", "'") + ";"
					}else{""}

				return colorPen(bef, regex, TAG) +
						s"""<span style="$fgColor$bgColor$fSize$fWeight$fFace$textDec">$content</span> """ +
						colorPen(fol, regex, TAG)
			}else{
				return colorPen(bef, regex, TAG) + colorPen(fol, regex, TAG)
			}
		}
		doc
	}

	private def generateTOC(doc:String, regex:String, TAG:String):String = {
	  	log info doc

		def _checkRange(start:Option[String],end:Option[String],default:Tuple2[Int,Int]):Tuple2[Int,Int] = {
			val s = if(start != None && start.get.toInt >= default._1){start.get.toInt}else{default._1}
			val e = if(end != None && end.get.toInt <= default._2){end.get.toInt}else{default._2}
			return Tuple2(s,e)
		}

		val text = ""
		val p = new Regex(regex, "before","toc","range","following")
		val m = p findFirstMatchIn(doc)
		var minmax = (1,6)
		if(m != None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")
			val tocRange = m.get.group("toc")
			if(Option(tocRange) != None){
				val p2 = s":h?([${minmax._1}-${minmax._2}])?\\-h?([${minmax._1}-${minmax._2}])?".r
				val range = p2 findFirstMatchIn(tocRange)
				if(range != None){
				  minmax = _checkRange(Option(range.get.group(1)),Option(range.get.group(2)),minmax)
				}else{
				  log warn "SYNTAX ERROR:toc header setting"
				}

			}

			val hList = makeHeaderMap(doc,minmax._1,minmax._2)
			for(e <- hList){
			  val headSize = e._2
			  val link = if(e._3 != None){e._3.get}else{s"""${e._2}:${e._4}:${e._1}""" }
			  headerMap ::= (e._1,headSize,link.toString,e._4)
			}
			headerMap = headerMap.reverse

			var i = headerMap.head._2
			var toc = ""
			var ulNest = 0
			for(h <- headerMap){
			  if(i < h._2){
			  	toc += """<ul style="list-style:none" >\\,"""
			  	ulNest += 1
			  }else if(i > h._2){
			    toc += """</ul>\\,""" * ulNest
			    ulNest = 0
			  }
			  toc += s"""<li><a href="#${h._3}" ><h${h._2}>${h._4}</h${h._2}></a></li>\\,"""
			  i = h._2
			}

			val table = """<header>\\,<ul style="list-style:none" id="toc"><nav>\\,""" + toc.mkString("") + "</nav></ul>\\,</header>"

			return putHeaderID(bef,minmax._1,minmax._2) + table + putHeaderID(fol,minmax._1,minmax._2)
		}

		doc
	}
	private def putHeaderID(doc:String,min:Int,max:Int):String ={
		if(doc == ""){return ""}
		var text = ""
		for((e,i) <- doc.split("""\\,""").zipWithIndex){
		    val p = """<h(\d)\s*?>(.*?)</h\d>""".r
		    val m = p findFirstMatchIn(e)

		    log debug "###" + e
		    if(m != None){
		    	val header = m.get.group(1).toInt
		    	if( header >= min && header <= max){
		    		val id = for(h<-headerMap if h._1 == i)yield{h._3}
		    		text += s"""<h${header} id="${id.head}">${m.get.group(2)}</h$header>\\,"""
		    	}else{
		    	  text += e + """\\,"""
		    	}
		    }else{text += e + """\\,"""}
		}
		text
	}

	private def makeHeaderMap(doc:String,minH:Integer,maxH:Integer):List[Tuple4[Integer,Integer,Option[String],String]] = {
		var headList = List[Tuple4[Integer,Integer,Option[String],String]]()
	  	for((line,no) <- doc.split("""\\,""").zipWithIndex){
	  		log info "line=" + line +" No=" + no
			val p = """.*?(<h(\d)(.*?)\s*?>(.*?)</h\d>).*""".r
			val m = p.findAllMatchIn(line)

			for(e <- m){
				val p2 = """<h(\d)(?:\s*?id=\"(.*?)\")?\s*?>(.*?)</h\d""".r
				val m2 = p2.findFirstMatchIn(e.group(1))
				val test = m2.get.group(1).toInt
				val id = m2.get.group(2)

				if(test >= minH && test <= maxH){
				  headList ::= (no, test ,Option(id),m2.get.group(3))
				}
			}
		}
		log info headList
		headList.reverse
	}

	private def wordDefinition(doc:String, regex:String, TAG:String):String = {
	    log info "***" + doc
		if(doc == ""){return ""}
		val p = new Regex(regex,"before","seq","word","defSeq","def","following")
		val m = p.findFirstMatchIn(doc)

		if(m != None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")
			val p2 = """(((?:\\,)?.*?\\,)(:.+?\\,)+)+?""".r
			log info "seq ->" + m.get.group("seq")

			val m2 = p2.findAllMatchIn(m.get.group("seq"))

			var dd = ""
			for(e <- m2){
				log debug "****" + e
				val p3 = """(?:\\,)?([^\\,]*?)\\,((:(.+?)\\,)+)""".r
				val m3 = p3.findAllMatchIn(e.group(1))
				for(e2 <- m3){
					log debug "###" + e2
					dd += "<dt>" + e2.group(1) + "</dt>"
					val p4 = """:(.+?)\\,""".r
					val m4 = p4.findAllMatchIn(e2.group(2))
					for(e3 <- m4){
						dd += "<dd>" + e3.group(1) + "</dd>\\,"
					}
				}
			}
			return wordDefinition(bef,regex,TAG) + "\\," +
				s"""<$TAG>\\,$dd\\,</$TAG>""" +
				wordDefinition(fol, regex,TAG)
		}
		doc
	}
	private def insertFootnote(doc:String, regex:String, TAG:String):String = {
		if(doc == ""){return ""}

		val p = new Regex(regex,"before","footnote","following")
		val m = p.findFirstMatchIn(doc)
		if(m != None){
			val bef = m.get.group("before")
			var fnote = m.get.group("footnote")
			val fol = m.get.group("following")
			if(footnoteMap.contains(fnote)){
				val link = footnoteMap(fnote)._1
				lazy val definition = footnoteMap(fnote)._2

				return insertFootnote(bef,regex,TAG) + s"""<$TAG id=\"$fnote\"><a href=\"#$link\" style="text-decoration:none">[$fnote]</a></sup>""" +
					insertFootnote(fol,regex,TAG)
			}else{
				log warn "Found lack of Footnotes."
			}
		}
		doc
	}

	private def gatheringFootnotesDefinition(doc:String):String = {
		val p = new Regex("""^(.*)\[\^(.*?)\]:(.*?)\\,(.*)$$""","before","landMark","definition","following")
		val m = p findFirstMatchIn(doc)

		if(m != None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")
			val lMark = m.get.group("landMark")
			val define = m.get.group("definition")

			if(!footnoteMap.contains(lMark)){
				footnoteMap += (lMark->("footnote-"+lMark,define))
			}else{
				log warn s"FOUND FOOTNOTE DUPES! @ $lMark: Second definitions was ignored."
			}
			return gatheringFootnotesDefinition(bef) + gatheringFootnotesDefinition(fol)
		}
		doc
	}

	private def insertFootnoteDefinitions(doc:LinkedHashMap[String,Tuple2[String,String]]):String = {
		lazy val head = """<footer><nav>\,<h2 id="footnotes">Footnotes</h2><hr />\,<ul style="list-style:none">\,"""
		val contents =
			for((key,t2) <- doc.toList.reverse)yield(s"""<li id="${t2._1}"><p><em>$key: </em>${t2._2}<a href="#$key">&laquo;</a></p>""")
		lazy val tail = "</ul></nav></footer>"
		if(contents.size > 0){
			return head + contents.mkString("\\,") + tail
		}else{""}
	}

	private def fencedCode(doc:String, regex:String, TAG:String):String = {
		val p = new Regex(regex, "before",  "SAttr", "inTAG", "following")
		val m = p.findFirstMatchIn(doc)

		if(m != None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")
			val inCode = m.get.group("inTAG")
			var specialAttr = ""

			if(Option(m.get.group("SAttr")) != None){
				specialAttr = decideClassOrStyle(doc,m.get.group("SAttr"))
			}

			return fencedCode(bef,regex,"code") +
				s"<pre $specialAttr><$TAG>\\\\," + ltgtExpand(inCode) + s"</$TAG></pre>" +
					fencedCode(fol,regex,TAG)
		}
		doc
	}

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
		val p = new Regex(regex, "before","caption","css","headSeq","head","separatorSeq","sep","bodySeq","body","b","following")
		val m = p findFirstMatchIn(doc)

		if(m != None){
			val bef = m.get.group("before")
			val fol = m.get.group("following")
			var head = m.get.group("headSeq")
			val sep = m.get.group("separatorSeq")
			val body = m.get.group("bodySeq")
			var cap = ""
			if(Option(m.get.group("caption")) != None){
				cap = s"""<caption>${m.get.group("caption")}</caption>"""
			}
			var id = ""
			if(Option(m.get.group("css")) != None){
				id = decideClassOrStyle(doc,m.get.group("css"))
			}

			if(Option(sep) != None){
				val pSep = """((?:\|)?(:?-{3,}?:?)(?:\|)?)+?""".r
				val mSep = pSep.findAllMatchIn(sep)

				var tableList = List[List[String]]()
				var tmpList = List[String]()
				for(mS <- mSep){
					val align = mS.group(2)
					if(align.startsWith(":") && align.endsWith(":")){
						tmpList ::= """align="center" """
					}else if(align.startsWith(":")){
						tmpList ::= """align="left" """
					}else if(align.endsWith(":")){
						tmpList ::= """align="right" """
					}else{
					  tmpList ::= ""
					}
				}
				val alignList = tmpList.reverse
				head = _normalize(head)
				log debug head
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
				var folTmp = ""

				tmpList = List.empty
				for((mTBS,i) <- mTBSeq.zipWithIndex){
					if(mTBS.group(2).contains("|")){
						val row = _normalize(mTBS.group(2)).split("\\|")
						val body =  for((c,j) <- row.zipWithIndex)yield(s"""<td ${_getAlign(alignList,j)}>$c</td>\\,""")
						bodyList ::= "<tr>\\\\," + body.mkString("") + "</tr>\\\\,"
						}else{
							folTmp += mTBS.group(2)
						}
				}

				bodyList = bodyList.reverse
				log debug bodyList
				return surroundTableTAG(bef, regex, TAG) +
					s"\\\\,<table $id>\\\\,$cap\\\\,<thead>\\\\," + s"<tr>${headList.mkString("")}</tr></thead>\\\\," +
					s"<tbody>${bodyList.mkString("")}</tbody></table>\\\\," +
					surroundTableTAG(folTmp + fol, regex, TAG)

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

			return autoMailLink(bef, regex, TAG) + "<address>" +
				s"""<script type=\"text/javascript\">\\,document.write('<$TAG href=\\"mailto:$mail')\\,document.write(\"@\")\\,document.write(\"$domain\\">MailMe!</$TAG>\") </script>""" + "</address>" +
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
	    				return _surroundByCodeTAG(bef, regex, "`", TAG) + s"<$TAG>" + ltgtExpand(m2.get.group(1)) + s"</$TAG>" +
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

		    log debug "doc ==>" + text
		    val p = new Regex("""^(.*?)?(((\[([\w\d\.\_\+\-\:\/)]+?)\]:([\w\d\.\_\+\-\:\/]+?)(?:\s+\"(.+?)\")?(?:\s+(\d+%?x\d+%?))?(?:\s+\{(.+?)\})?)\s*\\,)+?)(?:\\,|\z)(.*)?$$""",
				"before","seq","elem1","elem2","landMark","link","Title","Res","Css", "following")
		    val m = p findFirstMatchIn(text)

			if(m != None){
				if(m.get.group("before") != None){bef = m.get.group("before")}
				if(m.get.group("following") != None){fol = m.get.group("following")}

				log debug "bef=>" + bef
				log debug "seq=>" + m.get.group("seq")
				log debug "fol=>" + fol
				if(m.get.group("seq") != None){
					val seq = m.get.group("seq")
					val mat =
					"""\[([\w\d\.\_\+\-\:\/]+?)\]:([\w\d\.\_\+\-\:\/]+)(\s+\"(.+?)\")?(?:\s+(\d+%?x\d+%?))?(?:\s+\{(.+?)\})?(?:\s*\\,)?""".r.findAllMatchIn(seq)
					for(e <- mat){
						val link = e.group(2)
						log debug ">>" + link
						val landMark = e.group(1)
						log debug ">>>" + landMark
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
			if(className.contains(":")){
				return "style=\"" + className + "\""
			}else if(className.startsWith("#")){
				return "id=\"" + className + "\""
			}
			log warn s"$className not found..."
			return "class=\"" + className +"\""
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
				  ltgtExpand(contentStr) +
				  s"</$TAG></pre>\\," +
				  surroundByPreCodeTAG(fol, regex, TAG)
		}

	  doc
	}

	private def ltgtExpand(doc:String):String = {
		return doc.replaceAll("&","&amp").replaceAll("<","&gt;").replaceAll(">","&gt;")
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
	  val p = new Regex(regex, "before","inTAG","id","style","following")
	  val m = p findFirstMatchIn(doc)
	  var bef = ""
	  var fol = ""
	  var contentStr = ""
	  var headSign = TAG
	  var id = ""

	  if(m != None){
	    if(m.get.group("before") != None){bef = m.get.group("before")}else{bef = ""}
	    if(m.get.group("following") != None){fol = m.get.group("following")}else{fol = ""}
	    contentStr = m.get.group("inTAG")

	    if(m.get.group("style").contains("-")){
	      headSign += "2"
	    }else{
	      headSign += "1"
	    }

	    if(Option(m.get.group("id")) != None){
	    	id = s"""id="${m.get.group("id")}" """
	    }

	    return surroundByHeadTAGUnderlineStyle(bef, regex, TAG) +
			  s"<$headSign $id>$contentStr</$headSign>\\," + surroundByHeadTAGUnderlineStyle(fol, regex, TAG)
	  }
	  doc
	}

	private def surroundByHeadTAG(doc:String, regex:String, TAG:String):String = {
	  	if(doc == ""){return doc}

	  	log debug "--> " + doc
	  	val p = new Regex(regex, "before","startHead","inTAG","endHead","id","following")
		val m = p findFirstMatchIn(doc)
	  	var contentStr = ""

		if(m != None){
	      var bef = ""
	      var fol = ""
	      var id = ""

		  if(m.get.group("before") != None){bef = m.get.group("before")}else{bef = ""}
	      if(m.get.group("following") != None){fol = m.get.group("following")}else{fol = ""}
	      contentStr = m.get.group("inTAG")

		  val headSize = m.get.group("startHead").size
		  val endHead = m.get.group("endHead")
		  log debug "-->" + endHead
		  val headTAG = TAG + headSize
		  if(Option(m.get.group("id")) != None){val hashCode = m.get.group("id");id = s"""id="$hashCode" """}
		  if(Option(endHead) != None){
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
				  s"<${headTAG} $id>$contentStr</${headTAG}>\\," +
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
	 var text = urlDefinitions(doc)
	 text = gatheringFootnotesDefinition(text)
	 text = autoNumberSetting(text)
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
	  	log debug urlDefMap
	  	log debug footnoteMap
	  	md += insertFootnoteDefinitions(footnoteMap)
	  	val header = constructHEADER(markdown)
		s"${docType}\n${header}\n<${htmlTAG}>\n<${bodyTAG}>\n${md.replaceAll("\\\\,","\n")}\n</${bodyTAG}>\n</${htmlTAG}>"
	}

	def toDOM(markdown:String):org.w3c.dom.Document = {
		val stream = new ByteArrayInputStream(markdown.getBytes())

		val factory = DocumentBuilderFactory.newInstance
		factory.setNamespaceAware(true)
		val builder = factory.newDocumentBuilder()
		builder.parse(stream)
	}


	private def paragraphize(doc:String):String = {
		val delimiter = """\,"""
		def f(text:String):String = {
			text + delimiter
		}
		val BlockElements = new HTMLMap().BLOCKTags
		var isBlock = 0
		var isOneLineBlock = false
		var text = ""
		var pg = ""

		for(l <- doc.split("\\" + delimiter)){
			isOneLineBlock = false
			log debug l
			breakable{
				for(e <- BlockElements){
				  log debug e
					if(l.contains("<" + e) &&  l.contains("</" + e + ">")){
						isOneLineBlock = true;break;
					}else if(l.contains("<" + e)){
						isBlock += 1;break;
					}else if(l.contains("</" + e + ">")){
						isBlock -= 1;isOneLineBlock = true;break;
					}
				}
			}
			log debug ">>>>" + l + "::" + isBlock + "|" + isOneLineBlock
			if(isBlock > 0 | isOneLineBlock){
				text += l + delimiter
			}else{
				if(l != "" && isBlock <= 0){
					text += "<p>" + l + "</p>" + delimiter
				}
				isBlock = 0
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