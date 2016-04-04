/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package com.jaroop.modules.evolutions

import javax.inject._

import play.api.db.DBApi
import play.api.db.evolutions._
import play.api.inject.{ Injector, Module }
import play.api.{ Configuration, Environment }
import play.core.WebCommands

/**
 * Default module for evolutions API.
 */
class CoreEvolutionsModule extends EvolutionsModule {
  override def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[EvolutionsConfig].toProvider[DefaultEvolutionsConfigParser],
      bind[EvolutionsReader].to[EnvironmentEvolutionsReader],
      bind[EvolutionsApi].to[CoreEvolutionsApi],
      bind[ApplicationEvolutions].toProvider[ApplicationEvolutionsProvider].eagerly
    )
  }
}

@Singleton
class CoreEvolutionsApi @Inject() (dbApi: DBApi, schema: String) extends EvolutionsApi {

  private def databaseEvolutions(name: String, schema: String) = new DatabaseEvolutions(dbApi.database(name), "jaroop_core")

  def scripts(db: String, evolutions: Seq[Evolution], schema: String) = databaseEvolutions(db, "jaroop_core").scripts(evolutions)

  def scripts(db: String, reader: EvolutionsReader, schema: String) = databaseEvolutions(db, "jaroop_core").scripts(reader)

  def resetScripts(db: String, schema: String) = databaseEvolutions(db, "jaroop_core").resetScripts()

  def evolve(db: String, scripts: Seq[Script], autocommit: Boolean, schema: String) = databaseEvolutions(db, "jaroop_core").evolve(scripts, autocommit)

  def resolve(db: String, revision: Int, schema: String) = databaseEvolutions(db, "jaroop_core").resolve(revision)
}