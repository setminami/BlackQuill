package org.blackquill.engine

// BlackQuill Copyright (C) 2013 set.minami<set.minami@gmail.com>
// Lisence MIT see also LISENCE.txt
// (X)HTML Data Maps

import scala.collection.immutable.{HashSet,HashMap}

class HTMLMap{

  val HtmlTag = HashMap(
    "<html"->Tuple2("</html>",passThrough),
    "<head"->Tuple2("</head>",specialCharConvert _),
    "<body"->Tuple2("</body>",specialCharConvert _),
    "<!--"->Tuple2("-->",passThrough),
    "<title"->Tuple2("</title>",specialCharConvert _),
    "<isinindex"->Tuple2("</isinindex>",specialCharConvert _),
    "<base"->Tuple2("</base>",specialCharConvert _),
    "<meta"->Tuple2("</meta>",passThrough),
    "<link"->Tuple2("</link>",passThrough),
    "<script"->Tuple2("</script>",passThrough),
    "<hn"->Tuple2("</hn>",passThrough),
    "<style"->Tuple2(">",passThrough),
    "<hr />"->Tuple2(None,passThrough),
    "<br />"->Tuple2(None,passThrough),
    "<h1"->Tuple2("</h1>",specialCharConvert _),
    "<h2"->Tuple2("</h2>",specialCharConvert _),
    "<h3"->Tuple2("</h3>",specialCharConvert _),
    "<h4"->Tuple2("</h4>",specialCharConvert _),
    "<h5"->Tuple2("</h5>",specialCharConvert _),
    "<h6"->Tuple2("</h6>",specialCharConvert _),
    "<abbr"->Tuple2("</abbr>",specialCharConvert _),
    "<acronym"->Tuple2("</acronym>",specialCharConvert _),
    "<p"->Tuple2("</p>",specialCharConvert _),
    "<center"->Tuple2("</center>",specialCharConvert _),
    "<div"->Tuple2("</div>",specialCharConvert _),
    "<pre"->Tuple2("</pre>",passThrough),
    "<blockquote"->Tuple2("</blockquote>",specialCharConvert _),
    "<address"->Tuple2("</address>",passThrough),
    "<noscript"->Tuple2("</noscript>",passThrough),
    "<font"->Tuple2("</font>",specialCharConvert _),
    "<basefont />"->Tuple2(None,passThrough),
    "<i"->Tuple2("</i>",specialCharConvert _),
    "<tt"->Tuple2("</tt>",specialCharConvert _),
    "<abbr"->Tuple2("</abbr>",specialCharConvert _),
    "<b"->Tuple2("</b>",specialCharConvert _),
    "<u"->Tuple2("</u>",specialCharConvert _),
    "<strike"->Tuple2("</strike>",specialCharConvert _),
    "<big"->Tuple2("</big>",specialCharConvert _),
    "<small"->Tuple2("</small>",specialCharConvert _),
    "<sub"->Tuple2("</sub>",specialCharConvert _),
    "<sup"->Tuple2("</sup>",specialCharConvert _),
    "<em"->Tuple2("</em>",specialCharConvert _),
    "<strong"->Tuple2("<strong>",specialCharConvert _),
    "<code"->Tuple2("</code>",passThrough),
    "<dfn"->Tuple2("</dfn>",specialCharConvert _),
    "<samp"->Tuple2("</samp>",passThrough),
    "<kbd"->Tuple2("</kbd>",passThrough),
    "<var"->Tuple2("<var>",passThrough),
    "<ins"->Tuple2("</ins>",specialCharConvert _),
    "<del"->Tuple2("</del>",specialCharConvert _),
    "<cite"->Tuple2("</cite>",passThrough),
    "<ul"->Tuple2("</ul>",specialCharConvert _),
    "<ol"->Tuple2("</ol>",specialCharConvert _),
    "<li"->Tuple2("<li>",specialCharConvert _),
    "<dt"->Tuple2("</dt>",specialCharConvert _),
    "<dd"->Tuple2("</dd>",specialCharConvert _),
    "<object"->Tuple2("</object>",specialCharConvert _),
    "<table"->Tuple2("</table>",specialCharConvert _),
    "<tr"->Tuple2("</tr>",specialCharConvert _),
    "<td"->Tuple2("</td>",specialCharConvert _),
    "<caption"->Tuple2("</caption>",specialCharConvert _),
    "<a"->Tuple2("</a>",specialCharConvert _),
    "<img"->Tuple2(">",specialCharConvert _),
    "<map"->Tuple2("</map>",specialCharConvert _),
    "<area"->Tuple2(">",passThrough),
    "<form"->Tuple2("</form>",passThrough),
    "<input"->Tuple2(">",passThrough),
    "<select"->Tuple2("</select>",passThrough),
    "<option"->Tuple2("</option>",passThrough),
    "<textarea"->Tuple2("</textarea>",specialCharConvert _),
    "<applet"->Tuple2("</applet>",passThrough),
    "<param"->Tuple2("</param>",passThrough),
    "<frameset"->Tuple2("</frameset>",passThrough),
    "<frame"->Tuple2("</frame>",specialCharConvert _),
    "<noframes"->Tuple2("</noframes>",specialCharConvert _),
    "<bdo"->Tuple2("</bdo>",specialCharConvert _),
    "<span"->Tuple2("</span>",specialCharConvert _)
)

  val specialChar = HashMap("--" -> "&mdash;","<=" -> "&hArr;","<->" -> "&harr;",
    "->" ->"&rarr;","<-" ->"&larr;","=>" -> "&rArr;","<=" -> "&lArr;","||^" -> "&uArr;",
    "||/" -> "&dArr;","|/" -> "&darr;","|^" -> "&uarr;","+_" -> "&plusmn;","!=" -> "&ne;",
    "~=" -> "&cong;","<_" -> "&le;",">_" -> "&ge","|FA" -> "&forall;","|EX" -> "&exist;",
    "|=" -> "&equiv;","(+)" -> "&oplus;","(-)" -> "&ominus;","(X)" -> "&otimes;",
    "(c)" -> "&copy;","(R)" ->"&reg;","(SS)" -> "&sect;","(TM)" -> "&trade;",
    "!in" -> "&notin;")

  def passThrough = {}
  def specialCharConvert(text:String):String = {
    specialChar.keys.foreach(_.r.findFirstIn(text))
    text
  }
}


















