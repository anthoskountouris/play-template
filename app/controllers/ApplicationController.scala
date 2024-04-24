package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

// This class contains values necessary in most frontend controllers, such as multi-language support.
@Singleton
class ApplicationController @Inject() (val controllerComponents: ControllerComponents) extends BaseController{

  def index() = TODO
  /*
  TODO is a Play feature that is essentially a default page for controller actions
   that haven't been completed. It is a useful way of keeping your app functioning
   while building functionality incrementally.
   */

  def create() = TODO

  def read(id:String) = TODO

  def update(id:String) = TODO

  def delete(id:String) = TODO

}

