# Defining the Module
```
/**
 * Default module for evolutions API.
 */
class EvolutionsModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[EvolutionsConfig].toProvider[DefaultEvolutionsConfigParser],
      bind[EvolutionsReader].to[EnvironmentEvolutionsReader],
      bind[EvolutionsApi].to[DefaultEvolutionsApi],
      bind[ApplicationEvolutions].toProvider[ApplicationEvolutionsProvider].eagerly
    )
  }
}
```
The bindings here are binding interfaces to their implementations. In other words, `bind[EvolutionsConfig].toProvider[DefaultEvolutionsConfigParser]` means "the `EvolutionsConfig` trait is satisfied (and provided) by the `DefaultEvolutionsConfigParser`. We can see the definition of the trait here, in `ApplicationEvolutions.scala`:
```
/**
 * Evolutions configuration for a given datasource.
 */
trait EvolutionsDatasourceConfig {
  def enabled: Boolean
  def autocommit: Boolean
  def useLocks: Boolean
  def autoApply: Boolean
  def autoApplyDowns: Boolean
}
/**
 * Evolutions configuration for all datasources.
 */
trait EvolutionsConfig {
  def forDatasource(db: String): EvolutionsDatasourceConfig
}
```
---
`bind[EvolutionsReader].to[EnvironmentEvolutionsReader]` requires a concrete implementation of `EvolutionsReader` (defined in `EvolutionsApi.scala`), which just defines a method that returns a sequence of `Evolution` (`Evolutions.scala`) instances.
```
trait EvolutionsReader {
  /**
   * Read the evolutions for the given db
   */
  def evolutions(db: String): Seq[Evolution]
}

/**
 * An SQL evolution - database changes associated with a software version.
 *
 * An evolution includes ‘up’ changes, to upgrade to the next version, as well
 * as ‘down’ changes, to downgrade the database to the previous version.
 *
 * @param revision revision number
 * @param sql_up the SQL statements for UP application
 * @param sql_down the SQL statements for DOWN application
 */
case class Evolution(revision: Int, sql_up: String = "", sql_down: String = "") {

  /**
   * Revision hash, automatically computed from the SQL content.
   */
  val hash = sha1(sql_down.trim + sql_up.trim)

}
```
Then we have its implementation, which is:
```
/**
 * Read evolution files from the application environment.
 */
@Singleton
class EnvironmentEvolutionsReader @Inject() (environment: Environment) extends ResourceEvolutionsReader {

  def loadResource(db: String, revision: Int) = {
    environment.getExistingFile(Evolutions.fileName(db, revision)).map(new FileInputStream(_)).orElse {
      environment.resourceAsStream(Evolutions.resourceName(db, revision))
    }
  }
}
```
Notice that this class extends `ResourceEvolutionsReader`, which is where the `EvolutionsReader` trait is satisfied.

---
`bind[EvolutionsApi].to[DefaultEvolutionsApi]` uses the default evolutions API implementation:
```
/**
 * Default implementation of the evolutions API.
 */
@Singleton
class DefaultEvolutionsApi @Inject() (dbApi: DBApi) extends EvolutionsApi {

  private def databaseEvolutions(name: String) = new DatabaseEvolutions(dbApi.database(name))

  def scripts(db: String, evolutions: Seq[Evolution]) = databaseEvolutions(db).scripts(evolutions)

  def scripts(db: String, reader: EvolutionsReader) = databaseEvolutions(db).scripts(reader)

  def resetScripts(db: String) = databaseEvolutions(db).resetScripts()

  def evolve(db: String, scripts: Seq[Script], autocommit: Boolean) = databaseEvolutions(db).evolve(scripts, autocommit)

  def resolve(db: String, revision: Int) = databaseEvolutions(db).resolve(revision)
}
```


---

## Play 2.4 / 2.5
```
import play.api._
import play.api.db._
import play.api.db.evolutions._

// Start the application (instead of using `new play.core.StaticApplication(...)`)
val env = Environment(new java.io.File("."), this.getClass.getClassLoader, Mode.Dev)
val context = ApplicationLoader.createContext(env)
val loader = ApplicationLoader(context)
implicit val app = loader.load(context)

// Apply the evolutions
OfflineEvolutions.applyScript(
    new java.io.File("."),
    this.getClass.getClassLoader,
    app.injector.instanceOf[DBApi],
    "default",
    true
)
```

The evolutions normally run on construction of `ApplicationEvolutions`, which we need to somehow run twice so `jaroop_core_evolutions` run before `play_evolutions`.