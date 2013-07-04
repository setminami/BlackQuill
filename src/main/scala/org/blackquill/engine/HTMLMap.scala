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
    "header","hr","noframes","noscript","ol","p","pre","table","ul")

  private val HTMLTag = LinkedHashMap(
    "hr"->Tuple2((None),passThrough _),
    "br"->Tuple2((None),passThrough _),
    "!--"->Tuple2(("-->"),passThrough _),
    "link"->Tuple2((">"),specialCharConvert _),
    "style"->Tuple2((">"),passThrough _),
    "basefont"->Tuple2((">"),passThrough _),
    "html"->Tuple2(("</html>"),passThrough _),
    "head"->Tuple2(("</head>"),specialCharConvert _),
    "body"->Tuple2(("</body>"),specialCharConvert _),
    "title"->Tuple2(("</title>"),specialCharConvert _),
    "isinindex"->Tuple2(("</isinindex>"),specialCharConvert _),
    "base"->Tuple2(("</base>"),specialCharConvert _),
    "meta"->Tuple2(("</meta>"),passThrough _),
    "script"->Tuple2(("</script>"),passThrough _),
    "hn"->Tuple2(("</hn>"),passThrough _),
    "h1"->Tuple2(("</h1>"),specialCharConvert _),
    "h2"->Tuple2(("</h2>"),specialCharConvert _),
    "h3"->Tuple2(("</h3>"),specialCharConvert _),
    "h4"->Tuple2(("</h4>"),specialCharConvert _),
    "h5"->Tuple2(("</h5>"),specialCharConvert _),
    "h6"->Tuple2(("</h6>"),specialCharConvert _),
    "abbr"->Tuple2(("</abbr>"),specialCharConvert _),
    "acronym"->Tuple2(("</acronym>"),specialCharConvert _),
    "p"->Tuple2(("</p>"),specialCharConvert _),
    "center"->Tuple2(("</center>"),specialCharConvert _),
    "div"->Tuple2(("</div>"),specialCharConvert _),
    "pre"->Tuple2(("</pre>"),passThrough _),
    "blockquote"->Tuple2(("</blockquote>"),specialCharConvert _),
    "address"->Tuple2(("</address>"),passThrough _),
    "noscript"->Tuple2(("</noscript>"),passThrough _),
    "font"->Tuple2(("</font>"),specialCharConvert _),
    "i"->Tuple2(("</i>"),specialCharConvert _),
    "tt"->Tuple2(("</tt>"),specialCharConvert _),
    "abbr"->Tuple2(("</abbr>"),specialCharConvert _),
    "b"->Tuple2(("</b>"),specialCharConvert _),
    "u"->Tuple2(("</u>"),specialCharConvert _),
    "strike"->Tuple2(("</strike>"),specialCharConvert _),
    "big"->Tuple2(("</big>"),specialCharConvert _),
    "small"->Tuple2(("</small>"),specialCharConvert _),
    "sub"->Tuple2(("</sub>"),specialCharConvert _),
    "sup"->Tuple2(("</sup>"),specialCharConvert _),
    "em"->Tuple2(("</em>"),specialCharConvert _),
    "strong"->Tuple2(("<strong>"),specialCharConvert _),
    "code"->Tuple2(("</code>"),passThrough _),
    "dfn"->Tuple2(("</dfn>"),specialCharConvert _),
    "samp"->Tuple2(("</samp>"),passThrough _),
    "kbd"->Tuple2(("</kbd>"),passThrough _),
    "var"->Tuple2(("<var>"),passThrough _),
    "ins"->Tuple2(("</ins>"),specialCharConvert _),
    "del"->Tuple2(("</del>"),specialCharConvert _),
    "cite"->Tuple2(("</cite>"),passThrough _),
    "ul"->Tuple2(("</ul>"),specialCharConvert _),
    "ol"->Tuple2(("</ol>"),specialCharConvert _),
    "li"->Tuple2(("<li>"),specialCharConvert _),
    "dt"->Tuple2(("</dt>"),specialCharConvert _),
    "dd"->Tuple2(("</dd>"),specialCharConvert _),
    "object"->Tuple2(("</object>"),specialCharConvert _),
    "table"->Tuple2(("</table>"),specialCharConvert _),
    "tr"->Tuple2(("</tr>"),specialCharConvert _),
    "td"->Tuple2(("</td>"),specialCharConvert _),
    "caption"->Tuple2(("</caption>"),specialCharConvert _),
    "a"->Tuple2(("</a>",">"),specialCharConvert _),
    "img"->Tuple2((">"),specialCharConvert _),
    "map"->Tuple2(("</map>"),specialCharConvert _),
    "area"->Tuple2((">"),passThrough _),
    "form"->Tuple2(("</form>"),passThrough _),
    "input"->Tuple2((">"),passThrough _),
    "select"->Tuple2(("</select>"),passThrough _),
    "option"->Tuple2(("</option>"),passThrough _),
    "textarea"->Tuple2(("</textarea>"),specialCharConvert _),
    "applet"->Tuple2(("</applet>"),passThrough _),
    "param"->Tuple2(("</param>"),passThrough _),
    "frameset"->Tuple2(("</frameset>"),passThrough _),
    "frame"->Tuple2(("</frame>"),specialCharConvert _),
    "noframes"->Tuple2(("</noframes>"),specialCharConvert _),
    "bdo"->Tuple2(("</bdo>"),specialCharConvert _),
    "span"->Tuple2(("</span>"),specialCharConvert _),
    "NOTAG"->Tuple2(("---"),specialCharConvert _)
)

  private val specialChar = LinkedHashMap(
    """[^\-]\-\-[^\-+]""".r -> "&mdash;","""<=""".r -> "&hArr;","""<\->""".r -> "&harr;","""\->""".r ->"&rarr;","""<\-""".r ->"&larr;",
    """=>""".r -> "&rArr;","""<=""".r -> "&lArr;","""\|\|\^""".r -> "&uArr;",
    """\|\|/""".r -> "&dArr;","""\|/""".r -> "&darr;","""\|\^""".r -> "&uarr;","""\+\_""".r -> "&plusmn;","""!=""".r -> "&ne;",
    """~=""".r -> "&cong;","""<\_""".r -> "&le;",""">\_""".r -> "&ge","""\|FA""".r -> "&forall;","""\|EX""".r -> "&exist;",
    """\|=""".r -> "&equiv;","""\(\+\)""".r -> "&oplus;","""\(\-\)""".r -> "&ominus;","""\(X\)""".r -> "&otimes;",
    """\(c\)""".r -> "&copy;","""\(R\)""".r ->"&reg;","""\(SS\)""".r -> "&sect;","""\(TM\)""".r -> "&trade;",
    """!in""".r -> "&notin;", """\\<""".r->"&lt;","""\\>""".r->"&gt;","""\\&""".r->"&amp;")


  private def passThrough(text:String):String = {text}

  def specialCharConvert(text:String):String = {
    var str = text
    for(elem <- specialChar.keys){
      str = elem replaceAllIn(str,m => specialChar(elem))
    }
    str
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

  def htmlTAGFilter(doc:String):String = {
    if(doc == ""){return ""}
    val node = new BQParser


    val NORMALIZE: Regex = """(?i)(.+)""".r

    log debug "***" + doc
    for(elem <- HTMLTag keys){
      log debug HTMLTag(elem)._1
      val tmp = "</" + elem + ">"
      val endTag = HTMLTag(elem)._1

      for (eT <- Iterator(HTMLTag(elem) _1)){
    	eT match{
    	case None =>
    		val p = new Regex(s"""(?i)^(.*?)<${elem}\\s*[>|\\/>](.*?)$$""","before","following")
    		val m = p findFirstMatchIn(doc)
    		if(m != None){
    			return htmlTAGFilter(m.get.group("before")) + "<" +
    					elem + " />" + htmlTAGFilter(m.get.group("following"))
    		}
    	case NORMALIZE("-->") =>
    	  val p = new Regex(s"""^(.*?)<${elem}.*?${endTag}(.*?)$$""","before","following")
    	  val m = p findFirstMatchIn(doc)
    	  if(m != None){
    		return htmlTAGFilter(m.get.group("before")) + htmlTAGFilter(m.get.group("following"))
    	  }
    	case NORMALIZE(">") =>
          	val p = new Regex(s"""(?i)^(.*?)<${elem}\\s((\\w+=\\"(.*?)\\"\\s??)+)\\/??>(.*?)$$""",
          			"before","attributes","attribute","contents","following")
          	val m = p findFirstMatchIn(doc)
          	if(m != None){
          	  return htmlTAGFilter(m.get.group("before")) + "<" + elem + " " + m.get.group("attributes") + ">" +
          			  htmlTAGFilter(m.get.group("following"))

          	}
        case NORMALIZE(tmp) =>
        	val p = new Regex(
        			s"""(?i)^(.*?)<${elem}(.*?)>(.+?)${endTag}(.*?)$$""",
        			"before","attribute","inTAG","following")
        	val m = p findFirstMatchIn(doc)
        	if(m != None){
        		log debug "[" + elem + "]"
        		return htmlTAGFilter(m.get.group("before")) +
        				"<" + elem + m.get.group("attribute") + ">" +
        				HTMLTag(elem)._2(m.get.group("inTAG")) + endTag +
        				htmlTAGFilter(m.get.group("following"))
        	}
        case NORMALIZE("---") =>
        	val p = new Regex("""^(.*?)$$""","plain")
        	val m = p findFirstMatchIn(doc)
        	if(m != None){return node.toHTML(HTMLTag(elem)._2(m.get.group("plain"))).toString}
        case _ => return specialCharConvert(doc)
        //case NORMALIZE("""\\/>""") =>
        //  	log info "###"
    	}
      }
    }
    specialCharConvert(doc)

  }

}