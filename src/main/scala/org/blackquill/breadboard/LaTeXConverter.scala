package org.blackquill.breadboard

import uk.ac.ed.ph.snuggletex._
import org.blackquill.main._


class Latexconverter {
	private val texEngine:SnuggleEngine = new SnuggleEngine()
	private val session:SnuggleSession  = texEngine.createSession()

	def Convert(tex:String):String = {
		val input:SnuggleInput = new SnuggleInput("$" + tex.replaceAll("\\\\,","\n") + "$")
		val options = new XMLStringOutputOptions()
		options.setSerializationMethod(SerializationMethod.XHTML)
		options.setEncoding(BlackQuill.Switches.getEncoding)
		options.setAddingMathSourceAnnotations(true)
		options.setUsingNamedEntities(true)

		session.parseInput(input)
		session.buildXMLString(options)
	}
}