package com.lucidmouse.scaladi.data

/**
 * Created by: michal
 * 17.06.12, 20:56
 */

private[scaladi] class ContextDataOverrider(id: String, ctx: ContextData) extends ContextDataUpdater(id, ctx) {
  override protected val overrides = true
}
