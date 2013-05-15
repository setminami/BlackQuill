package org.blackquill
import org.specs2._


class BlackQuillSpec extends Specification {
  
  def is =
  sequential^
  "This is a spec to check status of Flags" ^
  "BlackQuill.main should" ^
  p^
  "When BlackQuill.main is given '--help'" ! before("--help") ^
  "'BlackQuill.Switches.stdin' must be False" ! isStdinFalse ^
  "'BlackQuill.Switches.stdout' must be False" ! isStdoutFalse ^
  "'BlackQuill.Switches.force' must be False" ! isForceFalse ^
  "'BlackQuill.Switches.output' must be False" ! isOutputFalse ^
  "'BlackQuill.Switches.verbose' must be False" ! isVerboseFalse ^
  "'BlackQuill.Switches.encFlag' must be False" ! isEncFlagFalse ^
  endp^
  p^
  "When BlackQuill.main is given '-h'" ! before("-h") ^
  "'BlackQuill.Switches.stdin' must be False" ! isStdinFalse ^
  "'BlackQuill.Switches.stdout' must be False" ! isStdoutFalse ^
  "'BlackQuill.Switches.force' must be False" ! isForceFalse ^
  "'BlackQuill.Switches.output' must be False" ! isOutputFalse ^
  "'BlackQuill.Switches.verbose' must be False" ! isVerboseFalse ^
  "'BlackQuill.Switches.encFlag' must be False" ! isEncFlagFalse ^
  endp^
  p^
  "When BlackQuill.main is given '--force'" ! before("--force") ^
  "'BlackQuill.Switches.stdin' must be False" ! isStdinFalse ^
  "'BlackQuill.Switches.stdout' must be False" ! isStdoutFalse ^
  "'BlackQuill.Switches.force' must be True" ! isForceTrue ^
  "'BlackQuill.Switches.output' must be False" ! isOutputFalse ^
  "'BlackQuill.Switches.verbose' must be False" ! isVerboseFalse ^
  "'BlackQuill.Switches.encFlag' must be False" ! isEncFlagFalse ^
  endp^
  p^
  "When BlackQuill.main is given '-f'" ! before("-f") ^
  "'BlackQuill.Switches.stdin' must be False" ! isStdinFalse ^
  "'BlackQuill.Switches.stdout' must be False" ! isStdoutFalse ^
  "'BlackQuill.Switches.force' must be True" ! isForceTrue ^
  "'BlackQuill.Switches.output' must be False" ! isOutputFalse ^
  "'BlackQuill.Switches.verbose' must be False" ! isVerboseFalse ^
  "'BlackQuill.Switches.encFlag' must be False" ! isEncFlagFalse ^
  endp^
  p^
  "When BlackQuill.main is gven '--stdout'" ! before("--stdout") ^
  "'BlackQuill.Switches.stdin' must be False" ! isStdinFalse ^
  "'BlackQuill.Switches.stdout' must be True" ! isStdoutTrue ^
  "'BlackQuill.Switches.force' must be False" ! isForceFalse ^
  "'BlackQuill.Switches.output' must be False" ! isOutputFalse ^
  "'BlackQuill.Switches.verbose' must be False" ! isVerboseFalse ^
  "'BlackQuill.Switches.encFlag' must be False" ! isEncFlagFalse ^
  endp^
  p^
  "When BlackQuill.main is given '-s'" ! before("-s") ^
  "'BlackQuill.Switches.stdin' must be False" ! isStdinFalse ^
  "'BlackQuill.Switches.stdout' must be True" ! isStdoutTrue ^
  "'BlackQuill.Switches.force' must be False" ! isForceFalse ^
  "'BlackQuill.Switches.output' must be False" ! isOutputFalse ^
  "'BlackQuill.Switches.verbose' must be False" ! isVerboseFalse ^
  "'BlackQuill.Switches.encFlag' must be False" ! isEncFlagFalse ^
  endp^
  p^
  "When BlackQuill.main is given '--enc shift-jis'" ! before("--enc shift-jis") ^
  "'BlackQuill.Switches.stdin' must be False" ! isStdinFalse ^
  "'BlackQuill.Switches.stdout' must be False" ! isStdoutFalse ^
  "'BlackQuill.Switches.force' must be False" ! isForceFalse ^
  "'BlackQuill.Switches.output' must be False" ! isOutputFalse ^
  "'BlackQuill.Switches.verbose' must be False" ! isVerboseFalse ^
  "'BlackQuill.Switches.encFlag' must be True" ! isEncFlagTrue ^
  "'BlackQuill.Switches.enc' must be shift-jis" ! isEncValid("shift-jis") ^
  endp^
  p^
  "When BlackQuill.main is given '--output foo'" ! before("--output foo") ^
  "'BlackQuill.Switches.stdin' must be False" ! isStdinFalse ^
  "'BlackQuill.Switches.stdout' must be False" ! isStdoutFalse ^
  "'BlackQuill.Switches.force' must be False" ! isForceFalse ^
  "'BlackQuill.Switches.output' must be True" ! isOutputTrue ^
  "'BlackQuill.Switches.dir' must be foo" ! isOutputValid("foo") ^
  "'BlackQuill.Switches.verbose' must be False" ! isVerboseFalse ^
  "'BlackQuill.Switches.encFlag' must be False" ! isEncFlagFalse ^
  "'BlackQuill.Switches.enc' must be UTF-8" ! isEncValid("UTF-8") ^
  endp^
  p^
  "When BlackQuill.main is given '--verbose'" ! before("--verbose") ^
  "'BlackQuill.Switches.stdin' must be False" ! isStdinFalse ^
  "'BlackQuill.Switches.stdout' must be False" ! isStdoutFalse ^
  "'BlackQuill.Switches.force' must be False" ! isForceFalse ^
  "'BlackQuill.Switches.output' must be False" ! isOutputFalse ^
  "'BlackQuill.Switches.verbose' must be True" ! isVerboseTrue ^
  "'BlackQuill.Switches.encFlag' must be False" ! isEncFlagFalse ^
  "'BlackQuill.Switches.enc' must be UTF-8" ! isEncValid("UTF-8") ^
  endp^
  p^
  "When BlackQuill.main is given '--version'" ! before("--version") ^
  "'BlackQuill.Switches.stdin' must be False" ! isStdinFalse ^
  "'BlackQuill.Switches.stdout' must be False" ! isStdoutFalse ^
  "'BlackQuill.Switches.force' must be False" ! isForceFalse ^
  "'BlackQuill.Switches.output' must be False" ! isOutputFalse ^
  "'BlackQuill.Switches.verbose' must be False" ! isVerboseFalse ^
  "'BlackQuill.Switches.encFlag' must be False" ! isEncFlagFalse ^
  "'BlackQuill.Switches.enc' must be UTF-8" ! isEncValid("UTF-8") ^
  endp^
  end

  def before(sw:String) = {
    BlackQuill.Switches.init
    val swArray = sw.split(" ")
    acceptSw(swArray)
    sw must startWith("-")
  }

  def acceptSw(sw:Array[String]) = {
    BlackQuill.main(sw)
  }

  def isStdinFalse = BlackQuill.Switches.stdin must beFalse

  def isStdoutFalse = BlackQuill.Switches.stdout must beFalse
  def isStdoutTrue = BlackQuill.Switches.stdout must beTrue

  def isForceFalse = BlackQuill.Switches.force must beFalse
  def isForceTrue = BlackQuill.Switches.force must beTrue

  def isOutputFalse = BlackQuill.Switches.output must beFalse
  def isOutputTrue = BlackQuill.Switches.output must beTrue
  def isOutputValid(dir:String) = BlackQuill.Switches.dirName must be matching(dir)

  def isVerboseFalse = BlackQuill.Switches.verbose must beFalse
  def isVerboseTrue = BlackQuill.Switches.verbose must beTrue

  def isEncFlagFalse = BlackQuill.Switches.encFlag must beFalse
  def isEncFlagTrue = BlackQuill.Switches.encFlag must beTrue
  def isEncValid(enc:String) = BlackQuill.Switches.encode must be matching(enc)

}



















