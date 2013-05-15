package org.blackquil.fileio


// BlackQuill Copyright (C) 2013 set.minami<set.minami@gmail.com>
// Lisence MIT see also LISENCE.txt
// FileI/O object.

import org.apache.commons.io.FileUtils
import org.apache.commons.logging._
import java.io.File

import scala.collection.JavaConversions._

import org.blackquill.BlackQuill


class FileIO{
  private val log:Log = LogFactory.getLog(classOf[FileIO])

  def openMarkdownFromFile(fileName:String){
    FileUtils.readLines(new File(fileName))
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




















