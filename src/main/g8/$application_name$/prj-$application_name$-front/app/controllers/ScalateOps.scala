package lib
import java.io.PrintWriter
import java.io.StringWriter

import play.api._
import play.api.mvc._

import http.Status._
import libs.json.JsValue

import core._
import nav.nav

import java.text.DecimalFormat
import http.{Writeable, ContentTypeOf, ContentTypes}
import mvc.Codec

import org.fusesource.scalate._
import org.fusesource.scalate.util._
import layout.{NullLayoutStrategy, DefaultLayoutStrategy}

import scalax.file.Path
import Path._
import scalax.file.PathSet
import scalax.file.PathMatcher._
import org.jboss.netty.handler.codec.serialization.ClassResolvers
import play.api.libs.json.Json._
import collection.mutable.ListBuffer
import util.FileResourceLoader
import util.Strings.isEmpty
import play.mvc.Results.Status

trait ScalateConfig {
  def format:String = "jade"
  val Playmode:Mode.Mode 
  def getFile(s:String):java.io.File
  def classloader: java.lang.ClassLoader
}

trait ScalatePlayConfig extends ScalateConfig {
  import play.api.Play.current
  import play.api.Configuration

  def conf: Configuration = Play.configuration

  override val format = conf.getString("scalate.format").getOrElse("jade")
  override val Playmode:Mode.Mode = Play.mode
  override def getFile(s:String):java.io.File = Play.getFile(s)
  override def classloader: java.lang.ClassLoader = Play.classloader
}



trait ScalateDefaultConfig extends ScalateConfig {
  override val Playmode = Mode.Dev
  override def getFile(s:String) = new java.io.File(s)
  override def classloader = this.getClass.getClassLoader()
  // new java.net.URLClassLoader()
  // import java.net.URL
  // java.net.URLClassLoader.newInstance(Array[URL](new URL("file://./target")))
}

object ScalateOps extends Scalate with ScalatePlayConfig

object ScalateCore extends Scalate with ScalateDefaultConfig

trait Scalate extends Results with ScalateConfig {

  type PlayRequest = play.api.mvc.Request[AnyContent]
  

  lazy val scalatePageEngine = {
    System.setProperty("scalate.workdir", "tmp")
    // val workdir = new java.io.File(System.getProperty("scalate.workdir", "tmp"))
    val engine = if (Playmode == Mode.Prod) {
      val tEng = new TemplateEngine(Seq(Path.fromString("app/controllers").jfile), "production")
      tEng.allowReload = false
      tEng.resourceLoader = new FileResourceLoader(Some(getFile("app/views")))
      tEng.layoutStrategy = new DefaultLayoutStrategy(tEng, ("layout/layout." + format))
      tEng.classpath = "target/scala-2.9.2/classes_managed:target/scala-2.9.2/classes" // Play.getFile("tmp/classes").getAbsolutePath
      tEng.workingDirectory = getFile("scalate-work-prod")
      tEng
    }
    else {
      val tEng = new TemplateEngine(Seq(Path.fromString("/app/controllers").jfile), "dev")
      tEng.allowReload = true
      tEng.resourceLoader = new FileResourceLoader(Some(getFile("/app/views")))
      tEng.layoutStrategy = new DefaultLayoutStrategy(tEng, getFile("/app/views/layout/layout." + format).getAbsolutePath)
      tEng.classpath = "scalate-work-dev~/classes:target/scala-2.9.2/classes" // getFile("tmp/classes").getAbsolutePath
      tEng.workingDirectory = getFile("scalate-work-dev~")
      tEng
    }
    engine.combinedClassPath = true
    engine.classLoader = classloader
    engine
  }


  val classResolver = ClassResolvers.softCachingConcurrentResolver(getClass.getClassLoader)

  def scalate(name: String)(args: (Symbol, Any)*): ScalateContent = scalate(name+"."+format,scalatePageEngine)(args:_*)


  def scalateMustache(name: String)(args: (Symbol, Any)*): ScalateContent = scalate(name+".mustache",scalateWidgetEngine)(args:_*)
  
//  def scalateWidget(name: String)(args: (Symbol, Any)*): ScalateContent =scalate(name,scalateWidgetEngine)(args:_*)
  
  def scalate(sourceUri: String, scalateEngine:TemplateEngine)(args: (Symbol, Any)*): ScalateContent = {
    val buffer = new StringWriter()
    val out = new PrintWriter(buffer)
    val context = createRenderContext(sourceUri, out, scalateEngine)

    scalateWithContext(sourceUri, scalateEngine,context)(args:_*)

    out.close()
    ScalateContent(buffer.toString)
  }

  def uriToClassname(sourceUri:String): String = {
    // quickstart/controller/AppController.jade  -> class scalate.quickstart.controller.$_scalate_$AppController_jade
    // quickstart/controller/Articles/index.jade -> class scalate.quickstart.controller.Articles.$_scalate_$index_jade


    // strip initial '/' if it exists
    val strippedUri = if (sourceUri.startsWith("/")) sourceUri.drop(1) else sourceUri
    // find extension(s)
    val pathParts = strippedUri.split("/")
    val baseFileName = pathParts.last


    // println("baseFilename: "+baseFileName)
    // strip ext(s)
    val baseFileParts = baseFileName.split('.')
    // println("baseFileParts: "+baseFileParts.mkString(", "))
    val baseFileNoExts = baseFileParts.head
    val exts = baseFileParts.drop(1)

    // val prefix = xs.take(xs.length - 2).mkString(".")
    val prefix = pathParts.dropRight(1).mkString(".")
    // println("prefix: " + prefix)
    val className = if (prefix.length > 0) prefix + ".$_scalate_$" + baseFileNoExts + "_" + exts.mkString("_")
                    else "$_scalate_$" + baseFileNoExts + "_" + exts.mkString("_")

    // println("className: " + className)
    try {
      val klass = classResolver.resolve(className)
      val template = klass.asInstanceOf[Class[Template]].newInstance()
    } catch {
      case e => // println("no class found for uri " + sourceUri)
    }

    className
  }

  def scalateWithContext(sourceUri: String, scalateEngine: TemplateEngine,context: DefaultRenderContext)(args: (Symbol, Any)*) {
    val attributes = args.map {
      case (k, v) => k.name -> v
    } toMap

    for ((key, value) <- attributes) {
      context.attributes(key) = value
    }

    context.attributes.update("context", context)
    // context.attributes.update(CONTROLLER_BINDING_ID, controller)
    // context.attributes("context") = context
//  /model/document/DocumentPoVComponent/$_scalate_$index_jade

    // scalateEngine.layout(scalateEngine.load(sourceUri, bindings), context)
    // scalateEngine.layout(scalateEngine.load(sourceUri), context)
    if (Playmode == Mode.Prod) {
      val classname = uriToClassname(sourceUri)
      val klass = classResolver.resolve(classname)
      val template = klass.asInstanceOf[Class[Template]].newInstance()
      //scalateEngine.layout(template, context)

      RenderContext.using(context) {
        //val source = template.source
        //if (source != null && source.uri != null) {
        //  context.withUri(source.uri) {
        //    scalateEngine.layoutStrategy.layout(template, context)
        //  }
        //}
        //else {
        scalateEngine.layoutStrategy.layout(template, context)
        // }
      }

    } else {
      uriToClassname(sourceUri)
      // println("dev render")
      scalateEngine.layout(scalateEngine.load(sourceUri), context)
      // val path = "/app/views" + java.io.File.separator + name
      // scalateEngine.layout(path, context)
    }
  }

  private def createRenderContext(uri: String, out: PrintWriter, engine: TemplateEngine): DefaultRenderContext = new DefaultRenderContext(uri, engine, out) {
    override def view(model : AnyRef, viewName : String) : Unit = {
      if (model == null) {
        throw new NullPointerException("No model object given!")
      }
      
      val templateUri: String = findViewUri(model,viewName,this)

      if (templateUri == null) {
        throw new NoSuchViewException(model, viewName)
      } else {
        using(model) {
          include(templateUri)
        }
      }
    }
  
  }

  case class ScalateContent(val cont: String)

  implicit def writeableOf_ScalateContent(implicit codec: Codec): Writeable[ScalateContent] = {
    Writeable[ScalateContent](scalate => codec.encode(scalate.cont))
  }

  implicit def contentTypeOf_ScalateContent(implicit codec: Codec): ContentTypeOf[ScalateContent] = {
    ContentTypeOf[ScalateContent](Some(ContentTypes.HTML))
  }

  lazy val scalateWidgetEngine = {
    System.setProperty("scalate.workdir", "tmp")
    // val workdir = new java.io.File(System.getProperty("scalate.workdir", "tmp"))
    val engine = if (Playmode == Mode.Prod) {
      val tEng = new TemplateEngine(Seq(Path.fromString("app/controllers").jfile), "production-widget")
      tEng.allowReload = false
      tEng.resourceLoader = new FileResourceLoader(Some(getFile("app/views")))
      tEng.layoutStrategy = NullLayoutStrategy
      tEng.classpath = "target/scala-2.9.2/classes_managed" // getFile("tmp/classes").getAbsolutePath
      tEng.workingDirectory = getFile("target/scala-2.9.2/src_managed/main")
      tEng
    }
    else {
      val tEng = new TemplateEngine(Seq(Path.fromString("/app/controllers").jfile), "dev-widget")
      tEng.allowReload = true
      tEng.workingDirectory = getFile("scalate-work-dev~widget-")
      tEng.resourceLoader = new FileResourceLoader(Some(getFile("/app/views")))
      tEng.layoutStrategy = NullLayoutStrategy
      tEng.classpath = getFile("/tmp/classes").getAbsolutePath
      tEng
    }
    engine.combinedClassPath = true
    engine.classLoader = classloader
    engine
  }


  def viewPage(it:AnyRef, view:String="index")(args: (Symbol, Any)*): ScalateContent = {
    val buffer = new StringWriter()
    val out = new PrintWriter(buffer)
    // buffer and out are totally unnecessary; we just want a context for the sake of the findViewUri
    val context = createRenderContext("", out, scalatePageEngine)
    val result = scalate(findViewUri(it,view,context),scalatePageEngine)((args:+ (('it,it))):_*)
    out.close()
    result
  }

  def viewPageWithErrorHandling(it:AnyRef, view:String="index")(args: (Symbol, Any)*): ScalateContent  = {

    val buffer = new StringWriter()
    val out = new PrintWriter(buffer)
    // buffer and out are totally unnecessary; we just want a context for the sake of the findViewUri
    val context = createRenderContext("", out, scalatePageEngine)

    val result = try {
      scalate(findViewUri(it,view,context),scalatePageEngine)((args:+ (('it,it))):_*)
    } catch {
      //case ex:TemplateNotFoundException => {
      //  if(ex.isSourceAvailable) {
      //    throw ex
      //
      //  }
      //  val element = PlayException.getInterestingStrackTraceElement(ex)
      //  if (element != null) {
      //    throw new TemplateNotFoundException(templateName, Play.classes.getApplicationClass(element.getClassName()), element.getLineNumber());
      //  } else {
      //    throw ex
      //  }
      //}
      case ex:InvalidSyntaxException => handleSpecialError(context,ex)
      case ex:CompilerException => handleSpecialError(context,ex)
      case ex:Exception => handleSpecialError(context,ex)
    } finally {
      out.close()

    }
    result
  }

  private def handleSpecialError(context:DefaultRenderContext,ex:Exception): ScalateContent = {
    context.attributes("javax.servlet.error.exception") = ex
    context.attributes("javax.servlet.error.message") = ex.getMessage
    try {
      /// def scalateWithContext(sourceUri: String, scalateEngine: TemplateEngine,context: RenderContext)(args: (Symbol, Any)*) {
      // val buffer = new StringWriter()
      // val out = new PrintWriter(buffer)
      // val context = createRenderContext("", out, scalatePageEngine)
      scalateWithContext("/status/500.scaml", scalatePageEngine, context)()
      // ScalateContent(buffer.toString)
      ScalateContent(context.out.toString())
    } catch {
      case ex:Exception =>
        // TODO use logging API from Play here...
        println("Caught: " + ex)
        ScalateContent("error!!")
    }
  }

  
  /*{
    val buffer = new StringWriter()
    val out = new PrintWriter(buffer)
    val context = new DefaultRenderContext("", scalatePageEngine, out)
    val attributes = args.map { case (k, v) => k.name -> v } toMap

    for ((key, value) <- attributes) {
      context.attributes(key) = value
    }
    context.attributes("it") = it
    context.attributes.update("context",    context)
    context.layout(findViewUri(it,view,context))()
    //context.view(it,view)
    out.close
    ScalateContent(buffer.toString)
  }*/
 
  
  def viewHtml(it:AnyRef, view:String="index")(args: (Symbol, Any)*) : String = {
    val buffer = new StringWriter()
    val out = new PrintWriter(buffer)
    // just use "dashboard" as a stub to get a valid render context
    val context = createRenderContext("dashboard", out, scalateWidgetEngine) //new DefaultRenderContext("dashboard", scalateWidgetEngine, out)
    
    viewWithArgs(it, view, context)(args:_*)
    
    out.close
    buffer.toString
    
  }


  def viewWithArgs(it:AnyRef, view:String="index", context:RenderContext)(args: (Symbol, Any)*) : Unit = {
  
    // store the original attribute map so we can restore it after this nested rendering
    val origAttributes = context.attributes.keySet.map(k=>(k, context.attributes(k))).toMap
    
    val attributes = args.map { case (k, v) => k.name -> v } toMap

    for ((key, value) <- attributes) {
      context.attributes(key) = value
    }
    context.attributes.update("context",    context)
    context.view(it,view)
    
    // replace the attribute map by brute force, since AttributeMap doesn't provide any handy methods for that
    val nestedKeys = context.attributeKeys
    for(k <- nestedKeys) { 
      context.attributes.remove(k) 
    }
    
    for ((key) <- origAttributes.keySet) {
      context.attributes.update(key, origAttributes(key))
    }
    context.attributes.update("context",    context)
  }
  
  
  def viewJson(it:AnyRef, view:String="index")(args: (Symbol, Any)*) = {
    val html = viewHtml(it,view)(args:_*)
      toJson(Map("html" -> html))
  }
  
  // use viewHtml to generate the fragments
  def viewJsonAndFragments(it:AnyRef, view:String="index")(args: (Symbol, Any)*)(fragments : Map[String,String],innerFragments : Map[String,String]) = {
    val html : String = viewHtml(it,view)(args:_*)
    val jsonFragments : Map[String, JsValue] = fragments.mapValues(s=>toJson(s))
    val jsonInnerFragments : Map[String, JsValue] = innerFragments.mapValues(s=>toJson(s))
    toJson(Map("html" -> toJson(html),"fragments" -> toJson(jsonFragments), "inner-fragments" ->toJson(jsonInnerFragments)))

    //toJson(Map("html" -> html, "fragments" -> fragments, "inner-fragments" -> innerFragments))
  }

  def viewFragments(fragments : Map[String,String],innerFragments : Map[String,String]) = {
    val jsonFragments : Map[String, JsValue] = fragments.mapValues(s=>toJson(s))
    val jsonInnerFragments : Map[String, JsValue] = innerFragments.mapValues(s=>toJson(s))
    toJson(Map("fragments" -> toJson(jsonFragments), "inner-fragments" ->toJson(jsonInnerFragments)))
  }
  
  def redirJson(url:String) = toJson(Map("location" -> url))

  val ValidationError = Ok

  val viewCache = scala.collection.mutable.Map[String, String]()
  
  def findViewUri(model:AnyRef,viewName:String="index",context:RenderContext) : String = {
    val modelClass = model.getClass
    val key = modelClass + viewName + context.toString()
    
    viewCache.get(key).getOrElse({
      val value = findViewUriByClasspathSearch(modelClass,viewName,context.viewPrefixes,context.viewPostfixes,context.engine)
      viewCache.put(key,value)
      value
    })   
  }

  // adapted from scalate/scalate-core/src/main/scala/org/fusesource/scalate/RenderContext.scala
  def findViewUriByClasspathSearch(modelClass:Class[_],viewName:String="index",viewPrefixes:List[String],viewPostfixes:List[String],engine:TemplateEngine) = {

    val classSearchList = new ListBuffer[Class[_]]()

    def buildClassList(clazz: Class[_]): Unit = {
      if (clazz != null && clazz != classOf[Object] && !classSearchList.contains(clazz)) {
        classSearchList.append(clazz);
        buildClassList(clazz.getSuperclass)
        for (i <- clazz.getInterfaces) {
          buildClassList(i)
        }
      }
    }

    def viewForClass(clazz: Class[_]): String = {
      for (prefix <- viewPrefixes; postfix <- viewPostfixes) {
        val path = clazz.getName.replace('.', '/') + "." + viewName + postfix
        val fullPath = if (isEmpty(prefix)) {"/" + path} else {"/" + prefix + "/" + path}
        if (engine.resourceLoader.exists(fullPath)) {
          return fullPath
        }
      }
      null
    }

    def searchForView(): String = {
      for (i <- classSearchList) {
        val rc = viewForClass(i)
        if (rc != null) {
          return rc;
        }
      }
      null
    }

    buildClassList(modelClass)
    val viewname = searchForView()
    // println("viewname = "+viewname)
    viewname

  }

}

