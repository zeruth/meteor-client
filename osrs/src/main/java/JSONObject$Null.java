import net.runelite.mapping.Export;
import net.runelite.mapping.Implements;
import net.runelite.mapping.ObfuscatedName;
import org.json.JSONObject;

@Implements("JSONObject$Null")
@ObfuscatedName("org/json/JSONObject$Null")
final class JSONObject$Null {

   JSONObject$Null() {
   }

   public final Object clone() {
      return this;
   }

   public String toString() {
      return "null";
   }

   @ObfuscatedName("equals")
   public boolean equals(Object var1) {
      return var1 == null || this == var1;
   }
}
