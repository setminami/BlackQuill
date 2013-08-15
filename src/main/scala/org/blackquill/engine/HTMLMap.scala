package org.blackquill.engine

// BlackQuill Copyright (C) 2013 set.minami<set.minami@gmail.com>
// License MIT see also LISENCE.txt
// HTML Maps.

import scala.collection.mutable.LinkedHashMap
import org.apache.commons.logging._
import scala.util.matching.Regex

class HTMLMap{

  private val log:Log = LogFactory.getLog(classOf[HTMLMap])

  val INLINETags = Set(
    "a","abbr","acronym","b","basefont","bdo","big","br","cite","code","dfn",
"em","font","i","img","input","kbd","label","q","s","samp","select",
"small","span","strike","strong","sub","sup","textarea","tt","u","var")

  val BLOCKTags = Set(
    "address","blockquote","center","div","dl","fieldset","form","h1","h2","h3","h4","h5","h6",
    "header","noframes","noscript","ol","p","pre","script","table","ul")

  private val HTMLTag = LinkedHashMap[String,(List[String],(String)=>String)](
    "hr"->Tuple2(List("xx"),passThrough _),
    "br"->Tuple2(List("xx"),passThrough _),
    "!--"->Tuple2(List("-->"),passThrough _),
    "link"->Tuple2(List(">"),specialCharConvert _),
    "style"->Tuple2(List(">"),passThrough _),
    "basefont"->Tuple2(List(">"),passThrough _),
    "html"->Tuple2(List("</html>"),passThrough _),
    "head"->Tuple2(List("</head>"),specialCharConvert _),
    "body"->Tuple2(List("</body>"),specialCharConvert _),
    "title"->Tuple2(List("</title>"),specialCharConvert _),
    "isinindex"->Tuple2(List("</isinindex>"),specialCharConvert _),
    "base"->Tuple2(List("</base>"),specialCharConvert _),
    "meta"->Tuple2(List("</meta>"),passThrough _),
    "script"->Tuple2(List("</script>"),passThrough _),
    "hn"->Tuple2(List("</hn>"),passThrough _),
    "h1"->Tuple2(List("</h1>"),specialCharConvert _),
    "h2"->Tuple2(List("</h2>"),specialCharConvert _),
    "h3"->Tuple2(List("</h3>"),specialCharConvert _),
    "h4"->Tuple2(List("</h4>"),specialCharConvert _),
    "h5"->Tuple2(List("</h5>"),specialCharConvert _),
    "h6"->Tuple2(List("</h6>"),specialCharConvert _),
    "abbr"->Tuple2(List("</abbr>"),specialCharConvert _),
    "acronym"->Tuple2(List("</acronym>"),specialCharConvert _),
    "p"->Tuple2(List("</p>"),specialCharConvert _),
    "center"->Tuple2(List("</center>"),specialCharConvert _),
    "div"->Tuple2(List("</div>"),specialCharConvert _),
    "pre"->Tuple2(List("</pre>"),passThrough _),
    "blockquote"->Tuple2(List("</blockquote>"),specialCharConvert _),
    "address"->Tuple2(List("</address>"),passThrough _),
    "noscript"->Tuple2(List("</noscript>"),passThrough _),
    "font"->Tuple2(List("</font>"),specialCharConvert _),
    "i"->Tuple2(List("</i>"),specialCharConvert _),
    "tt"->Tuple2(List("</tt>"),specialCharConvert _),
    "abbr"->Tuple2(List("</abbr>"),specialCharConvert _),
    "b"->Tuple2(List("</b>"),specialCharConvert _),
    "u"->Tuple2(List("</u>"),specialCharConvert _),
    "strike"->Tuple2(List("</strike>"),specialCharConvert _),
    "big"->Tuple2(List("</big>"),specialCharConvert _),
    "small"->Tuple2(List("</small>"),specialCharConvert _),
    "sub"->Tuple2(List("</sub>"),specialCharConvert _),
    "sup"->Tuple2(List("</sup>"),specialCharConvert _),
    "em"->Tuple2(List("</em>"),specialCharConvert _),
    "strong"->Tuple2(List("<strong>"),specialCharConvert _),
    "code"->Tuple2(List("</code>"),passThrough _),
    "dfn"->Tuple2(List("</dfn>"),specialCharConvert _),
    "samp"->Tuple2(List("</samp>"),passThrough _),
    "kbd"->Tuple2(List("</kbd>"),passThrough _),
    "var"->Tuple2(List("<var>"),passThrough _),
    "ins"->Tuple2(List("</ins>"),specialCharConvert _),
    "del"->Tuple2(List("</del>"),specialCharConvert _),
    "cite"->Tuple2(List("</cite>"),passThrough _),
    "ul"->Tuple2(List("</ul>"),specialCharConvert _),
    "ol"->Tuple2(List("</ol>"),specialCharConvert _),
    "li"->Tuple2(List("<li>"),specialCharConvert _),
    "dt"->Tuple2(List("</dt>"),specialCharConvert _),
    "dd"->Tuple2(List("</dd>"),specialCharConvert _),
    "object"->Tuple2(List("</object>"),specialCharConvert _),
    "table"->Tuple2(List("</table>"),specialCharConvert _),
    "tr"->Tuple2(List("</tr>"),specialCharConvert _),
    "td"->Tuple2(List("</td>"),specialCharConvert _),
    "caption"->Tuple2(List("</caption>"),specialCharConvert _),
    "a"->Tuple2(List("</a>",">"),specialCharConvert _),
    "img"->Tuple2(List(">"),specialCharConvert _),
    "map"->Tuple2(List("</map>"),specialCharConvert _),
    "area"->Tuple2(List(">"),passThrough _),
    "form"->Tuple2(List("</form>"),passThrough _),
    "input"->Tuple2(List(">"),passThrough _),
    "select"->Tuple2(List("</select>"),passThrough _),
    "option"->Tuple2(List("</option>"),passThrough _),
    "textarea"->Tuple2(List("</textarea>"),specialCharConvert _),
    "applet"->Tuple2(List("</applet>"),passThrough _),
    "param"->Tuple2(List("</param>"),passThrough _),
    "frameset"->Tuple2(List("</frameset>"),passThrough _),
    "frame"->Tuple2(List("</frame>"),specialCharConvert _),
    "noframes"->Tuple2(List("</noframes>"),specialCharConvert _),
    "bdo"->Tuple2(List("</bdo>"),specialCharConvert _),
    "span"->Tuple2(List("</span>"),specialCharConvert _),
    "NOTAG"->Tuple2(List("---"),specialCharConvert _)
)

  private val specialChar = LinkedHashMap(
    """[^\-]\-\-[^\-+]""".r -> "&mdash;","""<=""".r -> "&hArr;","""<\->""".r -> "&harr;","""\->""".r ->"&rarr;","""<\-""".r ->"&larr;",
    """=>""".r -> "&rArr;","""<=""".r -> "&lArr;","""\|\|\^""".r -> "&uArr;",
    """\|\|/""".r -> "&dArr;","""\|/""".r -> "&darr;","""\|\^""".r -> "&uarr;","""\+\_""".r -> "&plusmn;","""!=""".r -> "&ne;",
    """~=""".r -> "&cong;","""<\_""".r -> "&le;",""">\_""".r -> "&ge","""\|FA""".r -> "&forall;","""\|EX""".r -> "&exist;",
    """\|=""".r -> "&equiv;","""\(\+\)""".r -> "&oplus;","""\(\-\)""".r -> "&ominus;","""\(X\)""".r -> "&otimes;",
    """\(c\)""".r -> "&copy;","""\(R\)""".r ->"&reg;","""\(SS\)""".r -> "&sect;","""\(TM\)""".r -> "&trade;",
    """!in""".r -> "&notin;", """\\<""".r->"&lt;","""\\>""".r->"&gt;","""\\&""".r->"&amp;","""/\*""".r ->"/&lowast;","""\*/""".r -> "&lowast;/")


  private def passThrough(text:String):String = {text}

  def specialCharConvert(text:String):String = {
    var str = text
    for(elem <- specialChar.keys){
      str = elem replaceAllIn(str,m => specialChar(elem))
    }
    str
  }


  def specialCharConvert(text:List[String]):List[String] = {
    if(text.isEmpty){return text}
    var str = text.head
    for(elem <- specialChar.keys){
    	str = elem replaceAllIn(str,m => specialChar(elem))
    }
    return str::specialCharConvert(text.tail)
  }

/*
  def ampConverter(text:String):String = {
    val index = text.indexWhere(_ == '&',0)

    val headStr = text.slice(0,index)
    val subStr = text.slice(index,text.size)

    val amp : Regex = """^(.*?)(&(.+?)(;|\\,))(.*?)$$""".r
    subStr match{
      case amp(v1,v2,v3,v4,v5) =>
        if("\\,".equals(v4)){
          return headStr + (v1 + v2).replaceAll("&","&amp;") + ampConverter(v5)
        }else if(";".equals(v4)){
          return headStr + v1.replaceAll("&","&amp;") + v2 + ampConverter(v5)
        }
      case _ => return text
    }
    text
  }

  def gtConverter(text:String):String = {
   if(text.contains(">")){
     val index = text.indexWhere(_ == '>',0)
     if(index < 2){return text}

     val headStr = text.slice(0,index -2)
     val subStr = text.slice(index - 2,text.size)

     val gtSeq : Regex = """(.*?)(>+)(.*?)""".r
     subStr match{
       case  gtSeq(v1,v2,v3) =>
         log debug subStr
         log debug v1
         if(v1 == "\\,"){
           log debug "***->" + v2
           return headStr + v1 + v2 + gtConverter(v3)
         }else{
           return headStr + v1 + v2.replaceFirst(v2,"&gt;"*v2.size) + gtConverter(v3)
         }
       case _ =>
         return headStr + subStr
     }
   }else{return text}
 }

  def ltConverter(text:String):String ={
	text.replaceAll("<","&lt;")
  }
 */

  def htmlTAGFilter(text:String):String = {
    val start = System.currentTimeMillis()
    if(text == ""){return ""}
    val node = new BQParser
    val doc = text
    //log info "specialChar " + (System.currentTimeMillis() - start) + " msec"
    val NORMALIZE: Regex = """(?i)(.+)""".r

    log debug "***" + doc
    for(elem <- HTMLTag keys){
      log debug HTMLTag(elem)._1
      val tmp = "</" + elem + ">"
      val endTag = HTMLTag(elem)._1

      for (eT <- Iterator(HTMLTag(elem) _1)){
    	eT match{
    	case NORMALIZE("xx") =>
    		val p = s"""(?i)^(.*?)<${elem}\\s*(?:>|/>)(.*?)$$""".r("before","following")
    		val m = p findFirstMatchIn(doc)
    		if(m != None){
    		      log info "in TAGFilter None " + (System.currentTimeMillis() - start) + " msec"
    			return htmlTAGFilter(m.get.group("before")) + "<" +
    					elem + " />" + htmlTAGFilter(m.get.group("following"))
    		}
    	case NORMALIZE("-->") =>
    	  val p = s"""^(.*?)<${elem}.*?${endTag}(.*?)$$""".r("before","following")
    	  val m = p findFirstMatchIn(doc)
    	  if(m != None){
    	    log info "in TAGFilter --> " + (System.currentTimeMillis() - start) + " msec"
    		return htmlTAGFilter(m.get.group("before")) + htmlTAGFilter(m.get.group("following"))
    	  }
    	case NORMALIZE(">") =>
          	val p = s"""(?i)^(.*?)<${elem}\\s((\\w+=\\"(.*?)\\"\\s?)+)\\/?>(.*?)$$""".r(
          			"before","attributes","attribute","contents","following")
          	val m = p findFirstMatchIn(doc)
          	if(m != None){
          	      log info "in TAGFilter > " + (System.currentTimeMillis() - start) + " msec"
          	  return htmlTAGFilter(m.get.group("before")) + "<" + elem + " " + m.get.group("attributes") + ">" +
          			  htmlTAGFilter(m.get.group("following"))

          	}
        case NORMALIZE(tmp) =>
        	val p = s"""(?i)^(.*?)<${elem}(.*?)>(.+?)${endTag}(.*?)$$""".r(
        			"before","attribute","inTAG","following")
        	val m = p findFirstMatchIn(doc)
        	if(m != None){
        		log debug "[" + elem + "]"
        		log info "in TAGFilter NORMALIZE " + (System.currentTimeMillis() - start) + " msec"
        		return htmlTAGFilter(m.get.group("before")) +
        				"<" + elem + m.get.group("attribute") + ">" +
        				HTMLTag(elem)._2(m.get.group("inTAG")) + endTag +
        				htmlTAGFilter(m.get.group("following"))
        	}
        case NORMALIZE("---") =>
        	val p = """^(.*?)$$""".r("plain")
        	val m = p findFirstMatchIn(doc)
        	if(m != None){
        	  log info "in TAGFilter NORMALIZE --- " + (System.currentTimeMillis() - start) + " msec"
        	  return node.toHTML(HTMLTag(elem)._2(m.get.group("plain"))).toString
        	}
        case _ => return doc
        //case NORMALIZE("""\\/>""") =>
        //  	log info "###"
    	}
      }
    }

    return doc

  }

}