package filters

import javax.inject.Inject

import org.apache.commons.lang3.StringUtils
import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter

class GZipFilter @Inject() extends HttpFilters {
  def filters = Seq(new GzipFilter(shouldGzip = (request, response) => {
    println("Accepted Types :" + request.acceptedTypes)
    request.headers.get("Accept-Encoding").exists(StringUtils.containsIgnoreCase(_, "gzip"))
  }))
}
