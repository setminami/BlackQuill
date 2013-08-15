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

import java.io.PrintWriter
import java.io.File

object BlackQuill{
  private val log:Log = LogFactory.getLog(BlackQuill.getClass)

  val VERSION = "0.1.6"
  val lastDate = "Augus 15 2013"

  val wiki = "https://www.setminami.net/BlackQuill/"
  val syntax = "index.html#Syntax"
  val philosophy = "index.html#Philosophy"

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

    var outputFile = ""

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
    val sufRegex = """\.(md|markdown|txt|bq|blackquill)$$""".r
    val start = System.currentTimeMillis()

    val it = args.iterator
      for(elem <- it){
        log debug "=>" + elem.toString
        if(elem.startsWith("--")){
          elem.toString() match {
            case "--force" => Switches.setForce(true)
            case "--stdout" => Switches.setStdout(true)
            case "--enc" => Switches.setEncoding(true,it.next.toString)
            case "--output" => Switches.setOutput(true,it.next.toString)
            case "--verbose" => Switches.setVerbose(true)
            case "--version" => log.info("BlackQuill Version" + VERSION + " updated at " + lastDate)
            case "--help" =>
              log info description;exit
            case _ => log warn s"Wrong switch is found. $elem";exit()
          }
        }else if(elem.startsWith("-")){
          for(e <- elem){
            e match {
              case '-' => print()
              case 'f' => Switches.setForce(true)
              case 's' => Switches.setStdout(true)
              case 'v' => Switches.setVerbose(true)
              case _ => log warn "Wrong Switch is found.";exit()
            }
          }
        }else{
          if(elem == "org.blackquill.main"){
            println()
          }else if(sufRegex.findFirstMatchIn(elem.toString) != None){
            Switches.setInputfile(elem.toString)
          }else {
            log warn "inputfile suffix is wrong. => .md|.markdown|.txt|.bq|.blackquill"
          }
        }
      }
      val fileHandler = FileIO
      Switches.outputFile = sufRegex.replaceAllIn(Switches.getInputfile,".html")
      val iCheck = new File(Switches.getInputfile)
      val oCheck = new File(Switches.outputFile)
      // - fileHandler.openMarkdownFromString(str:String)
      if(Switches.getForce || !oCheck.exists ||oCheck.lastModified < iCheck.lastModified){
        val text:List[String] = fileHandler openMarkdownFromFile(Switches.getInputfile,Switches.getEncoding)
        val output = blackquill(new HTMLMap().specialCharConvert(text))
        if(Switches.getVerbose){
         log info "generate HTML " + (System.currentTimeMillis() - start) + " msec"
        }

          val out:PrintWriter = if(Switches.getStdout){
              new PrintWriter(System.err)
            }else{
              if(Switches.getOutput){
                new PrintWriter(Switches.getOutputDir + Switches.outputFile,Switches.getEncoding)
              }else{
                new PrintWriter(Switches.dirName + Switches.outputFile,Switches.getEncoding)
              }
            }
        //log info output
        output.foreach(out.print(_))
        out.close
        if(Switches.getVerbose){
          log info (System.currentTimeMillis() - start) + " msec"
          val chkdStr = if(oCheck.length > 1073741824){
            oCheck.length/1073741824 + " GB"
          }else if(oCheck.length > 1048576){
            oCheck.length/1048576 + " MB"
          }else if(oCheck.length > 1024){
            oCheck.length/1024 + " KB"
          }else{
            oCheck.length + " B"
          }
          log info oCheck.getName() + " => " + chkdStr
        }
      }else{
        log warn "The MarkDown file is not changed. (see also --help)"
      }
  }

  def blackquill(lines:List[String]):List[String] = {
    val start = System.currentTimeMillis()
    val str = new HTMLMap htmlTAGFilter lines.mkString("""\,""")
    if(Switches.getVerbose){
      log info "TAGFiltered " + (System.currentTimeMillis() - start) + " msec"
    }
    log debug str
    val HTML = new BQParser toHTML(str)
    if(Switches.getVerbose){
      log info "toHTML " + (System.currentTimeMillis() - start) + " msec"
    }
    log debug HTML
    HTML split """\,""" toList
  }

 }
