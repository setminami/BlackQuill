package org.blackquill.engine
import scala.collection.immutable.HashMap
import scala.collection.mutable.LinkedHashMap
import org.apache.commons.logging._
import scala.util.matching.Regex

class HTMLMap{
  
  private val log:Log = LogFactory.getLog(classOf[HTMLMap])

  val HTMLTag = HashMap(
    "NOTAG"->Tuple2("---",specialCharConvert _),
    "html"->Tuple2("</html>",passThrough _),
    "head"->Tuple2("</head>",specialCharConvert _),
    "body"->Tuple2("</body>",specialCharConvert _),
    "!--"->Tuple2("-->",passThrough _),
    "title"->Tuple2("</title>",specialCharConvert _),
    "isinindex"->Tuple2("</isinindex>",specialCharConvert _),
    "base"->Tuple2("</base>",specialCharConvert _),
    "meta"->Tuple2("</meta>",passThrough _),
    "link"->Tuple2("</link>",specialCharConvert _),
    "script"->Tuple2("</script>",passThrough _),
    "hn"->Tuple2("</hn>",passThrough _),
    "style"->Tuple2(">",passThrough _),
    "hr"->Tuple2(None,passThrough _),
    "br"->Tuple2(None,passThrough _),
    "h1"->Tuple2("</h1>",specialCharConvert _),
    "h2"->Tuple2("</h2>",specialCharConvert _),
    "h3"->Tuple2("</h3>",specialCharConvert _),
    "h4"->Tuple2("</h4>",specialCharConvert _),
    "h5"->Tuple2("</h5>",specialCharConvert _),
    "h6"->Tuple2("</h6>",specialCharConvert _),
    "abbr"->Tuple2("</abbr>",specialCharConvert _),
    "acronym"->Tuple2("</acronym>",specialCharConvert _),
    "p"->Tuple2("</p>",specialCharConvert _),
    "center"->Tuple2("</center>",specialCharConvert _),
    "div"->Tuple2("</div>",specialCharConvert _),
    "pre"->Tuple2("</pre>",passThrough _),
    "blockquote"->Tuple2("</blockquote>",specialCharConvert _),
    "address"->Tuple2("</address>",passThrough _),
    "noscript"->Tuple2("</noscript>",passThrough _),
    "font"->Tuple2("</font>",specialCharConvert _),
    "basefont />"->Tuple2(None,passThrough _),
    "i"->Tuple2("</i>",specialCharConvert _),
    "tt"->Tuple2("</tt>",specialCharConvert _),
    "abbr"->Tuple2("</abbr>",specialCharConvert _),
    "b"->Tuple2("</b>",specialCharConvert _),
    "u"->Tuple2("</u>",specialCharConvert _),
    "strike"->Tuple2("</strike>",specialCharConvert _),
    "big"->Tuple2("</big>",specialCharConvert _),
    "small"->Tuple2("</small>",specialCharConvert _),
    "sub"->Tuple2("</sub>",specialCharConvert _),
    "sup"->Tuple2("</sup>",specialCharConvert _),
    "em"->Tuple2("</em>",specialCharConvert _),
    "strong"->Tuple2("<strong>",specialCharConvert _),
    "code"->Tuple2("</code>",passThrough _),
    "dfn"->Tuple2("</dfn>",specialCharConvert _),
    "samp"->Tuple2("</samp>",passThrough _),
    "kbd"->Tuple2("</kbd>",passThrough _),
    "var"->Tuple2("<var>",passThrough _),
    "ins"->Tuple2("</ins>",specialCharConvert _),
    "del"->Tuple2("</del>",specialCharConvert _),
    "cite"->Tuple2("</cite>",passThrough _),
    "ul"->Tuple2("</ul>",specialCharConvert _),
    "ol"->Tuple2("</ol>",specialCharConvert _),
    "li"->Tuple2("<li>",specialCharConvert _),
    "dt"->Tuple2("</dt>",specialCharConvert _),
    "dd"->Tuple2("</dd>",specialCharConvert _),
    "object"->Tuple2("</object>",specialCharConvert _),
    "table"->Tuple2("</table>",specialCharConvert _),
    "tr"->Tuple2("</tr>",specialCharConvert _),
    "td"->Tuple2("</td>",specialCharConvert _),
    "caption"->Tuple2("</caption>",specialCharConvert _),
    "a"->Tuple2("</a>",specialCharConvert _),
    "img"->Tuple2(">",specialCharConvert _),
    "map"->Tuple2("</map>",specialCharConvert _),
    "area"->Tuple2(">",passThrough _),
    "form"->Tuple2("</form>",passThrough _),
    "input"->Tuple2(">",passThrough _),
    "select"->Tuple2("</select>",passThrough _),
    "option"->Tuple2("</option>",passThrough _),
    "textarea"->Tuple2("</textarea>",specialCharConvert _),
    "applet"->Tuple2("</applet>",passThrough _),
    "param"->Tuple2("</param>",passThrough _),
    "frameset"->Tuple2("</frameset>",passThrough _),
    "frame"->Tuple2("</frame>",specialCharConvert _),
    "noframes"->Tuple2("</noframes>",specialCharConvert _),
    "bdo"->Tuple2("</bdo>",specialCharConvert _),
    "span"->Tuple2("</span>",specialCharConvert _)
)

  val specialChar = LinkedHashMap(
    """\-\-""".r -> "&mdash;","""<=""".r -> "&hArr;","""<\->""".r -> "&harr;","""\->""".r ->"&rarr;","""<\-""".r ->"&larr;",
    """=>""".r -> "&rArr;","""<=""".r -> "&lArr;","""\|\|^""".r -> "&uArr;",
    """\|\|/""".r -> "&dArr;","""\|/""".r -> "&darr;","""\|^""".r -> "&uarr;","""\+\_""".r -> "&plusmn;","""!=""".r -> "&ne;",
    """~=""".r -> "&cong;","""<\_""".r -> "&le;",""">\_""".r -> "&ge","""\|FA""".r -> "&forall;","""\|EX""".r -> "&exist;",
    """\|=""".r -> "&equiv;","""\(\+\)""".r -> "&oplus;","""\(\-\)""".r -> "&ominus;","""\(X\)""".r -> "&otimes;",
    """\(c\)""".r -> "&copy;","""\(R\)""".r ->"&reg;","""\(SS\)""".r -> "&sect;","""\(TM\)""".r -> "&trade;",
    """!in""".r -> "&notin;", """<""".r->"&lt;",""">""".r->"&gt;")

  private def passThrough(text:String):String = {text}
  private def specialCharConvert(text:String):String = {
    var str = text
    for(elem <- specialChar.keys){
      str = elem replaceAllIn(str,m => specialChar(elem))
    }
    str
  }


  def htmlTAGFilter(doc:String):String = {
    val NORMALIZE: Regex =  """(.+)""".r

    log info "***" + doc
    for(elem <- HTMLTag keys){
      log debug HTMLTag(elem)._1
      val tmp = "</" + elem + ">"
      val endTag = HTMLTag(elem)._1

      HTMLTag(elem) _1 match{
        case None =>
          val p = new Regex(s"""(?i)^(.*?)<${elem}\\s*[>|.>](.*?)$$""","before","following")
          val m = p findFirstMatchIn(doc)
          if(m != None){
            return htmlTAGFilter(m.get.group("before")) + "<" +
            		elem + " />" + htmlTAGFilter(m.get.group("following"))
            		
          }
        case NORMALIZE(tmp) =>
          val p = new Regex(
            s"""(?i)^(.*?)<${elem}(.*?)>(.+?)${endTag}(.*?)$$""",
            "before","attribute","inTAG","following")
          val m = p findFirstMatchIn(doc)
          if(m != None){
            log info "[" + elem + "]"
            return htmlTAGFilter(m.get.group("before")) + 
            "<" + elem + m.get.group("attribute") + ">" + 
            HTMLTag(elem)._2(m.get.group("inTAG")) + endTag + 
            htmlTAGFilter(m.get.group("following"))
          }
        case NORMALIZE(">") => log info "%%%"
        case NORMALIZE("---") => 
          val p = new Regex("""(!?<.+?>)^(.*)$$""","plain")
          val m = p findFirstMatchIn(doc)
          if(m != None){return HTMLTag(elem)._2(doc)}          
        case _ => log info "000"
      }
    }
    specialCharConvert(doc):String
  }

}

















