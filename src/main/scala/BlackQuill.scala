package org.blackquill

// BlackQuill Copyright (C) 2013 set.minami<set,minami@gmail.com>
// Lisence MIT see also LISENCE.txt
// Main object and set Switches.

import scala.collection.JavaConversions._
import scala.util.matching._

object BlackQuill{
  val VERSION = "0.1.0"
  val lastDate = "May 14 2013"

  val wiki = "https://github.com/setminami/BlackQuill/wiki/"
  val syntax = "BlackQuill-Details-of-Syntax"
  val philosophy = "BlackQuill-Philosophy"

  val options = 
    "--force|-f : Force conversion. BQ ignore timestamps of markdown files.\n" +
    "--stdout|-s :BQ outputs document to STDOUT as HTML.\n" + 
    "--enc shift-jis|euc-jp|UTF-8|ASCII default input enc is UTF-8\n" +
    "- : BQ accept markdown document from STDIN\n"  +
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
    def getInputfile{inputFile}

    def setForce(b:Boolean){force = b}
    def getForce{force}
    def setStdout(b:Boolean){stdout = b}
    def getStdout{stdout}
    def setStdin(b:Boolean){stdin = b}
    def getStdin{stdin}
    def setOutput(b:Boolean,dir:String){output = b;dirName = dir}
    def getOutput{output}
    def getOutputDir{dirName}
    def setVerbose(b:Boolean){verbose = b}
    def getVerbose{verbose}
    def setEncoding(b:Boolean,enc:String){encFlag=b;encode = enc}
    def getEncoding{encode}

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
        println("=>" + elem.toString)
        elem.toString() match {
          case "--force"|"-f" => Switches.setForce(true)
          case "--stdout"|"-s" => Switches.setStdout(true)
          case "--enc" => Switches.setEncoding(true,it.next.toString)
          case "--output" => Switches.setOutput(true,it.next.toString)
          case "--verbose"|"-v" => Switches.setVerbose(true)
          case "--version" => println("BlackQuill Version" + VERSION + " updated at " + lastDate)
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
    }catch{ case e:Exception => println("wrong switch is found see --help or Website\n" + wiki)}
  }


}















