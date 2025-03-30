package org.ldemetrios.astra.samples.calculator

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.ldemetrios.astra.api.ParsingException
import org.ldemetrios.astra.samples.expr.ExprLexer
import org.ldemetrios.astra.samples.expr.ExprParser
import org.ldemetrios.astra.samples.expr.Ordinal
import org.ldemetrios.astra.samples.expr.OrdinalsLexer
import org.ldemetrios.astra.samples.expr.OrdinalsParser
import java.lang.AssertionError
import kotlin.math.sin

class OrdinalParserTest : FreeSpec({
    fun eval(text: String): Ordinal {
        val parser = OrdinalsParser(OrdinalsLexer(text))
        return parser.expr()
    }

    "Priorities" {
        eval("1 + 2 * 3 ^ 4").toString() shouldBe "163"
    }

    "Recursion" {
        eval("(((((((2)))))))").toString() shouldBe "2"
    }

    "Complex" {
        eval("(3^0)^w*0*w+(w*(w+(4*w^2+w))+1^(w+1)^(w*1))").toString().replace(" ", "") shouldBe "w^(3)+w^(2)+1"
        eval("3*(w*(w+w*4)+w)*((0^w)^(0*w*(w*(w+w^((w+1^(w+1))^((w+(1+0*w^(1*0)+w)+(w+1)+(2^(((0+w)*1)^(w+(1+(w+0))*w)*2*w))^(w*(w+w*1*(w*2))))^w*0)*(1+(w+2)))*w*(w*2)))))^w*(w+(w+(w+w*4*(w*((1+4+w)^(((w+(w+2)+w)*(w+1)+2)*w))^(w*(w+2*(w+(w+0)^w+(w+1))))+(w+((w+(((0^(w*1^(w*0*(1+2)+w)+(w+2)))^w)^(w+(w+2^w))^(w*0))^(0*w*(w^(((0^w+1^0)^w+1)*2)+(w*0+w))*((w^1)^(w+0+(0+w)+(w+(w+2)))*w*(w*2^(2+w)*0*w)*(w*2*w+(w^1+0))*w)))*(w+3)*4+w)^(0*w)))+w*(w+w*((w^0+(3+2))*(w+0*w^(w+((w+w*0^w*(w+0))*((1*2*w)^2+w)+(w+w*1+2)*(1*(2*w)+1)*(w*0)+w))+w*(w*(w*(w*1)*(w*4+(4+w)))*2)^w*((w^(w+1*0)*w*(w*(w+0)))^(1+2)*w)*(2*w*(w*3)+4^w+4)*(w*((0*w+(4^w)^(w*2))*w+((w+3)*(1*4+w)*(4*w)+(w+2*w*4))*w+0))*w))*(w+(w+(w+1+(w+0)^w*3)*((3+3*((w*(2*(w+1+w*1))+2*(w*3+2)*w^(w^1+w*0))*(w*((w+0*w)*(w+2)))*(((w+1)^1^1)^1+3+(w*2)^w*(w*(4^w)^(w+1))+2))+(w*((w+2)*((w*(w*4)*(3^2+w))^(1*2*(w*2)*w)*1*(0*w))))^0^w)*3))))*(w+3)*((w+4^(w*(2*w)*(2*w))^2+w+(w+0))*w))*1))+(w+w*4*w^0^w)^(w*0*(w*0))").toString()
            .replace(" ", "") shouldBe "w^(w^(w)+w^(5)+w^(4)*2)+w^(w^(5)+1)+1"
    }
})
