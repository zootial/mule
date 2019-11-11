<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mule="http://www.mulesoft.org/schema/mule">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>
    <xsl:strip-space elements="*"/>

    <!-- Identity transform -->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Drop content of all body elements, we just want the interface of the smart connector-->
    <xsl:template match="/mule:mule/mule:operation/mule:body/*" />

    <!-- Drop all elements but those that are <operation/>s -->
    <xsl:template match="/mule:mule/*">
        <xsl:if test="name()='operation'">
            <xsl:copy>
                <xsl:apply-templates select="@*|node()"/>
            </xsl:copy>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
