package fixture.consumer

import com.github.richardwrq.krouter.annotation.Inject

class ConsumerTarget {
    @field:Inject(name = "payload")
    var payload: ConsumerPayload? = null
}
