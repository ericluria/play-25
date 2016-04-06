## Play 2.3.x => 2.5.x upgrade notes

What I've tried so far:

- Rewriting the evolutions plugin in play to accept an `evolutionsTable` option in the database configuration. I followed [this pull request](https://github.com/playframework/playframework/pull/5485) that introduced the `schema` option, but got stuck trying to fix Java code that somehow broke after editing the Scala code.
- Rewriting [this line](https://github.com/playframework/playframework/blob/master/framework/src/play-jdbc-evolutions/src/main/scala/play/api/db/evolutions/EvolutionsApi.scala#L316-L318) which also occurs [here](https://github.com/playframework/playframework/blob/9a06b9a0a6ea7d1299e61ea3c1f37a08b714297e/framework/src/play-jdbc-evolutions/src/main/scala/play/api/db/evolutions/ApplicationEvolutions.scala#L154-L156), to instead append an underscore to the schema name. This way, we could just (ab)use the `schema` option to generate separate `${schema}_play_evolutions` table, and customize it in `application.conf`. I think this idea _might_ work, but it's hacky, and we'd have to figure out how to run multiple sets of evolutions in order.
- Copying all of the code relevant to evolutions from Play into my own project and calling the evolutions module twice. This idea got messy very quickly, so I abandoned it.
- Using [flyway-play](https://github.com/flyway/flyway-play) as a replacement for Play evolutions. One of the benefits of Flyway is that it has hooks for all steps of the migration process; however, based on [this issue](https://github.com/flyway/flyway-play/pull/12/files) I'm not sure if the plugin has support for callbacks. My intention was to use these callbacks to run sets of migrations in a particular order, but I soon realized that it probably won't work.

## Lessons Learned

##### Starting a new application in console
In Play 2.4.x and 2.5.x, one can no longer start an application in console using `implicit val app = new play.core.StaticApplication(new java.io.File("."))`. Instead, based on [this StackOverflow answer](http://stackoverflow.com/a/34584295/2297665), we need to do the following. I also included code for running evolutions manually:
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
    true,
    { optional schema name goes here, which is really referring to a database - default ""}
)
```

##### Test Play 2.5 Application
I have a repo up [here](https://github.com/ericluria/play-25) with an example 2.5 project, but you'll need to change the Play version since it's currently referencing a locally-published snapshot.