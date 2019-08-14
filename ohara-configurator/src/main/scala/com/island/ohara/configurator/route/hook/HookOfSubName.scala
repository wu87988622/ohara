/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.island.ohara.configurator.route.hook

import com.island.ohara.common.setting.ObjectKey

import scala.concurrent.Future

/**
  * The basic interface of handling the request carrying the sub name (/$name/$subName)
  */
private[route] trait HookOfSubName {
  def apply(key: ObjectKey, subName: String, params: Map[String, String]): Option[Future[Unit]]
}

/**
  * a collection of action hooks.
  * The order of calling hook is shown below.
  * /$name/$action?group=${group}
  * 1) the hook which has key composed by (group, name)
  * 2) the hook
  * 3) return NotFound
  *
  * For cluster resource, the "action" may be a node name that the request is used to add/remove node from cluster.
  */
private[route] object HookOfSubName {
  val empty: HookOfSubName = (_, _, _) => None

  def apply(hook: HookOfAction): HookOfSubName = builder.hook(hook).build()

  def apply(hooks: Map[String, HookOfAction]): HookOfSubName = {
    val builder = new Builder
    hooks.foreach {
      case (action, hook) => builder.hook(action, hook)
    }
    builder.build()
  }

  def builder: Builder = new Builder
  private[route] class Builder private[HookOfSubName] extends com.island.ohara.common.pattern.Builder[HookOfSubName] {
    private[this] var hooks: Map[String, HookOfAction] = Map.empty
    private[this] var finalHook: Option[HookOfAction] = None

    /**
      * add the hook for particular action from PUT method.
      * @param action action
      * @param hook hook
      * @return this builder
      */
    def hook(action: String, hook: HookOfAction): Builder = {
      if (hooks.contains(action)) throw new IllegalArgumentException(s"the action:$action already has hook")
      this.hooks += (action -> hook)
      this
    }

    /**
      * set the hook for all actions from PUT method
      * @param hook hook
      * @return this builder
      */
    def hook(hook: HookOfAction): Builder = {
      this.finalHook = Some(hook)
      this
    }

    override def build(): HookOfSubName = (key: ObjectKey, subName: String, params: Map[String, String]) =>
      hooks.get(subName).orElse(finalHook).map(_(key, subName, params))
  }
}
