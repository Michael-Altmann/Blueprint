package jahirfiquitiva.libs.quest.events

import jahirfiquitiva.libs.quest.App

import java.util.ArrayList

/**
 * Created by Allan Wang on 2016-08-27.
 */
class AppLoadedEvent(val apps: ArrayList<App>?, val exception: Exception?) {
    fun hasException(): Boolean = exception != null
}