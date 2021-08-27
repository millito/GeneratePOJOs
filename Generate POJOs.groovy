import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "com.mepu.sample;"
typeMapping = [
        (~/(?i)int/)                      : "Long",
        (~/(?i)float|double|decimal|real/): "Double",
        (~/(?i)datetime|timestamp/)       : "java.util.Date",
        (~/(?i)date/)                     : "java.util.Date",
        (~/(?i)time/)                     : "java.util.Date",
        (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    def className = javaName(table.getName(), true)
    def tableName = table.getName()
    def fields = calcFields(table)
    new File(dir, className + ".java").withPrintWriter { out -> generate(out, tableName,className, fields) }
}

def generate(out,tableName,className, fields) {
    out.println "package $packageName"
    out.println ""
    out.println "import io.swagger.annotations.ApiModel;"
    out.println "import io.swagger.annotations.ApiModelProperty;"
    out.println "import lombok.Data;"
    out.println "import lombok.ToString;"
    out.println ""
    out.println "import javax.persistence.Column;"
    out.println "import javax.persistence.Table;"
    out.println "import java.io.Serializable;"
    out.println ""
    out.println ""
    out.println "@Data"
    out.println "@ToString"
    out.println "@ApiModel(description = \"$className\")"
    out.println "@Table(name = \"$tableName\")"
    out.println "public class $className implements Serializable {"
    out.println ""
    fields.each() {
        if (it.annos != "") out.println "  ${it.annos}"
        out.println "    @ApiModelProperty(value = \"\")"
        out.println "    @Column(name = \"${it.sqlName}\")"
        out.println "    private ${it.type} ${it.name};"
        out.println ""
    }
    out.println ""
//    fields.each() {
//        out.println ""
//        out.println "    public ${it.type} get${it.name.capitalize()}() {"
//        out.println "        return ${it.name};"
//        out.println "    }"
//        out.println ""
//        out.println "    public void set${it.name.capitalize()}(${it.type} ${it.name}) {"
//        out.println "        this.${it.name} = ${it.name};"
//        out.println "    }"
//        out.println ""
//    }
    out.println "}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           name : javaName(col.getName(), false),
                           sqlName : col.getName(),
                           type : typeStr,
                           annos: ""]]
    }
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
