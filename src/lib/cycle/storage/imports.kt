@file:JsModule("@cycle/storage")
package lib.cycle.storage

import lib.cycle.DriverFunction

@JsName("default")
external val storageDriver: DriverFunction<StorageRequest, Any>
