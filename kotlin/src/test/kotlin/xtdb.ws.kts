import xtdb.api.XtdbClient
import xtdb.api.query.Binding
import xtdb.api.query.Exprs.expr
import xtdb.api.query.Exprs.pullMany
import xtdb.api.query.Queries
import xtdb.api.query.Queries.aggregate
import xtdb.api.query.Queries.from
import xtdb.api.query.Queries.limit
import xtdb.api.query.Queries.orderBy
import xtdb.api.query.Queries.orderSpec
import xtdb.api.query.Queries.pipeline
import xtdb.api.query.Queries.returning
import xtdb.api.query.Queries.unify
import xtdb.api.query.Queries.with
import xtdb.api.query.QueryOptions
import xtdb.api.query.queryOpts
import java.net.URL

XtdbClient.openClient(URL("http://localhost:3010")).use { xtdb ->
    xtdb.openQuery(
        pipeline(
            unify(
                from("film_actor") { bindAll("filmId", "actorId") },
                from("actor") {
                    bind("xt/id", expr { "actorId".sym })
                    bindAll("firstName", "lastName")
                }
            ),

            aggregate {
                bindAll("firstName", "lastName")
                bind("filmCount", expr { "rowCount"() })
            },

            aggregate {
                bindAll("filmCount")
                bind("frequency", expr { "rowCount"() })
            },

            orderBy(orderSpec("filmCount")),
        ),
    ).use {
        println(it.toList().joinToString("\n"))
    }
}
