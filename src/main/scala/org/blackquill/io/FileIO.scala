package org.blackquill.io


// BlackQuill Copyright (C) 2013 set.minami<set.minami@gmail.com>
// Lisence MIT see also LISENCE.txt
// FileI/O object.

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.logging._
import java.io.File
import scala.collection.JavaConversions._
import org.blackquill.main.BlackQuill
import java.io.FileNotFoundException

object FileIO{
  private val log:Log = LogFactory.getLog(FileIO.getClass)
  private var baseDir = ""

  def openMarkdownFromFile(fileName:String,encoding:String):List[String] = {
    try{
        baseDir = FilenameUtils getFullPath fileName
        FileUtils.readLines(new File(fileName),encoding).toList
        }catch{
          case e:FileNotFoundException  =>
          log error "File Not Found :" + fileName
          exit(-1)
          case e:IllegalArgumentException =>
          log error "Illegal Char Code :" + encoding
          exit()
        }
  }

  def openCSSFile(fileName:String,encoding:String):List[String] = {
    val p = """^(?i)(file:|http|\/|\w:\\).*$$""".r
    val m = p findFirstMatchIn(fileName)
    if(m != None){
      openMarkdownFromFile(fileName,encoding)
    }else{
      openMarkdownFromFile(baseDir + fileName,encoding)
    }
  }

  def openMarkdownFromString(str:String){
    str.split("\n").toList
  }

  def writeHtml(fileName:String,html:String){
    val BQS = BlackQuill.Switches
    var path = ""
    if(BQS.getOutput){path = BQS.getOutputDir + fileName}else{path = fileName}

    val fileTester = new File(path)
    val markdownTester = new File(BlackQuill.Switches.getInputfile)

    if(BQS.getStdout){println(html)}
    else if(fileTester.lastModified > markdownTester.lastModified){
      if(BQS.getForce){
        FileUtils.writeStringToFile(fileTester,html,BQS.getEncoding,false)
      }
    }else{
       FileUtils.writeStringToFile(fileTester,html,BQS.getEncoding,false)
    }
  }

}

