package org.blackquill.main

// BlackQuill Copyright (C) 2013 set.minami<set.minami@gmail.com>
// License MIT see also LISENCE.txt
// Main object and set Switches.

import scala.collection.JavaConversions._
import scala.xml._
import scala.util.matching._
import org.apache.commons.logging._
import org.blackquill.engine._
import org.blackquill.io._

object BlackQuill{
  private val log:Log = LogFactory.getLog(BlackQuill.getClass)

  val VERSION = "0.1.0"
  val lastDate = "May 14 2013"

  val wiki = "https://github.com/setminami/BlackQuill/wiki/"
  val syntax = "BlackQuill-Details-of-Syntax"
  val philosophy = "BlackQuill-Philosophy"

  val options = 
    "--force|-f : Force conversion. BQ ignore timestamps of markdown files.\n" +
    "--stdout|-s :BQ outputs document to STDOUT as HTML.\n" + 
    "--enc shift-jis|euc-jp|UTF-8|ASCII default input enc is UTF-8\n" +
    "--output DIR|-o :BQ outputs HTML to under DIR\n" +
    "--verbose|-v :output conversion processes verbosely \n" +
    "--version :output version and so on.\n" + 
    "--help|-h :output usage descriptions\n" +
    "...and  Markdown file's suffix is .md|.markdown|.txt|.bq|.BlackQuill\n" +
    "e.g., BlackQuill --force foo.md"


  val description = "Welcome to BlackQuill.\n" +
    "BQ switches=> \n" + options +
    "\nPlease see also... \n" +
    wiki + syntax + "\n" +
    wiki + philosophy + "\n" 

  object Switches{
    def setInputfile(input:String){inputFile = input}
    def getInputfile:String={inputFile}

    def setForce(b:Boolean){force = b}
    def getForce:Boolean={force}
    def setStdout(b:Boolean){stdout = b}
    def getStdout:Boolean={stdout}
    def setStdin(b:Boolean){stdin = b}
    def getStdin:Boolean={stdin}
    def setOutput(b:Boolean,dir:String){output = b;dirName = dir}
    def getOutput:Boolean={output}
    def getOutputDir:String={dirName}
    def setVerbose(b:Boolean){verbose = b}
    def getVerbose:Boolean={verbose}
    def setEncoding(b:Boolean,enc:String){encFlag=b;encode = enc}
    def getEncFlag:Boolean={encFlag}
    def getEncoding:String={encode}

    def init{
      inputFile = ""
      force = false
      stdout = false
      stdin = false
      output = false
      dirName = ""
      verbose = false
      encFlag = false
      encode = "UTF-8"
    }

    private
    var inputFile = ""
    var force = false

    var stdout = false

    var stdin = false

    var output = false
    var dirName = ""

    var verbose = false
    var encFlag = false
    var encode = "UTF-8"


  }

  def main(args:Array[String]){
    val sufRegex = """(\.md$)|(\.markdown$)|(\.txt$)|(\.bq$)|(\.blackquill$)""".r

    try{
      val it = args.iterator
      for(elem <- it){
        log.debug("=>" + elem.toString)
        elem.toString() match {
          case "--force"|"-f" => Switches.setForce(true)
          case "--stdout"|"-s" => Switches.setStdout(true)
          case "--enc" => Switches.setEncoding(true,it.next.toString)
          case "--output" => Switches.setOutput(true,it.next.toString)
          case "--verbose"|"-v" => Switches.setVerbose(true)
          case "--version" => log.info("BlackQuill Version" + VERSION + " updated at " + lastDate)
          case "--help"|"-h" =>
            println(description)
          case _ => 
            if(sufRegex.findFirstIn(elem.toString) != None){
              Switches.setInputfile(elem.toString)
            }else{
              throw new RuntimeException
            }
        }
      }
    }catch{ case e:Exception => log.warn("wrong switch is found see --help or Website\n" + wiki)}
    val fileHandler = new FileIO
    // - fileHandler.openMarkdownFromString(str:String)
    val text:List[String] = fileHandler openMarkdownFromFile(Switches.getInputfile)
    val output = blackquill(text)
    log info output
  }

  def blackquill(lines:List[String]):List[String] = {
    val str = new HTMLMap htmlTAGFilter lines.mkString("\\,")
    val parsed = new BQParser
    log info parsed.toHTML(str)
     str split """\\,""" toList
  }

}
