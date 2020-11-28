package ru.hardmet.memologio
package http

import java.time.format.DateTimeFormatter

package object routes {
  private[routes] val Pattern = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy HH:mm")
}
