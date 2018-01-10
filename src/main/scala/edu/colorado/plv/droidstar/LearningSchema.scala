package edu.colorado.plv.droidstar

abstract class LearningSchema(c: Context) {
  private[this] var context: Context = c
  private[this] var forOutput: Callback = null

  def logl(m: String) = log("PURPOSE", m)

  def logcb(output: String) = logl("Callback reported: " + output)

  def respond(output: String) = {
    if (forOutput != null) {
      logcb(output);
      forOutput.handleMessage(quickMessage(output));
    } else {
      throw new AssertionError(
        "LP's callback was not initialized.  You must call reset() before you use it."
      )
    }
  }

  def reset(c: Callback): String {
    forOutput = c
    val flag: String = resetActions(context, c)
    logl("LP has been reset.")
    flag
  }

  def inputSet(): List[String] {
    NotImplemented
  }
}
