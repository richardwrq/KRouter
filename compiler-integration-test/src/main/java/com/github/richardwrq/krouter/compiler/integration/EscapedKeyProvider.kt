package com.github.richardwrq.krouter.compiler.integration

import android.content.Context
import com.github.richardwrq.krouter.annotation.Provider
import com.github.richardwrq.krouter.api.interfaces.IProvider

internal const val ESCAPED_PROVIDER_KEY = "provider/quote\"/backslash\\/line\n雪"

@Provider(ESCAPED_PROVIDER_KEY)
class EscapedKeyProvider : IProvider {
    override fun init(context: Context) = Unit
}
