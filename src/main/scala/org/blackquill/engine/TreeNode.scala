package org.blackquill.engine

import scala.collection.immutable.Traversable
import scala.collection.immutable.List
import scala.collection.mutable.ArrayBuffer

class TreeNode[T](val content:T) extends Traversable[T] {
	override def toString="%s".format(content.toString)

    var parent :Option[TreeNode[T]]=None
    val children = ArrayBuffer[TreeNode[T]]()

    def add(child :TreeNode[T])={
        child.parent=Some(this)
        children.append(child)
        child
    }
  	
 	def getContents():T = {
 	  return content
 	} 	
 	
 	def getChildren:List[TreeNode[T]] = {
 	  val childrenList = children.toList
 	  childrenList
 	}

    def foreach[U](f: T => U){
        for(child <- children){
            f(child.content)
            child.foreach(f)
        }
    }
    
    override def mkString(sep:String):String = {
      children.mkString(sep)
    }

}