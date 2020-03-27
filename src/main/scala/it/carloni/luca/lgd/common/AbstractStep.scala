package it.carloni.luca.lgd.common

import it.carloni.luca.lgd.common.utils.{ScalaUtils, LGDCommons}
import it.carloni.luca.lgd.common.udfs.{SparkUDFs, UDFsNames}
import org.apache.commons.configuration.{ConfigurationException, PropertiesConfiguration}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.log4j.Logger

abstract class AbstractStep extends StepTrait {

  private val logger: Logger = Logger.getLogger(getClass)
  private val lgdProperties: PropertiesConfiguration = new PropertiesConfiguration
  private val sparkSession: SparkSession = getSparkSessionWithUDFs

  // TRY TO LOAD PROPERTIES
  try

    lgdProperties.load(getClass.getClassLoader.getResourceAsStream("lgd.properties"))

  catch {
    case exception: ConfigurationException =>
      logger.error("ConfigurationException occurred")
      logger.error(s"exception.getMessage: ${exception.getMessage}")
      logger.error(exception)
  }

  private val csvFormat: String = LGDCommons.CSV.SparkCsvFormat
  private val csvInputDelimiter: String = LGDCommons.CSV.InputDelimiter
  private val csvOutputDelimiter: String = LGDCommons.CSV.OutputDelimiter

  logger.debug(s"csvFormat : $csvFormat")
  logger.debug(s"csvInputDelimiter: $csvInputDelimiter")
  logger.debug(s"csvOutputDelimiter:  $csvOutputDelimiter")

  protected def getPropertyValue(propertyName: String): String = lgdProperties.getString(propertyName)

  private def getSparkSessionWithUDFs: SparkSession = registerUDFS(SparkSession.builder().getOrCreate())

  private def registerUDFS(sparkSession: SparkSession): SparkSession = {

    sparkSession.udf.register(UDFsNames.AddDurationUDFName, SparkUDFs.addDurationUDF)
    sparkSession.udf.register(UDFsNames.ChangeDateFormatUDFName, SparkUDFs.changeDateFormat)
    sparkSession.udf.register(UDFsNames.SubtractDurationUDFName, SparkUDFs.subtractDurationUDF)
    sparkSession.udf.register(UDFsNames.IsDateGeqOtherDateUDFName, SparkUDFs.isDateGeqOtherDateUDF)
    sparkSession.udf.register(UDFsNames.IsDateLeqOtherDateUDFName, SparkUDFs.isDateLeqOtherDateUDF)
    sparkSession.udf.register(UDFsNames.LeastDateUDFName, SparkUDFs.leastDateUDF)
    sparkSession
  }

  protected def fromPigSchemaToStructType(columnMap: Map[String, String]) = new StructType(columnMap.map(ScalaUtils.getTypedStructField).toArray)

  protected def readCsvFromPathUsingSchema(csvPath: String, schema: StructType): DataFrame =
    sparkSession.read.format(csvFormat).option("sep", csvInputDelimiter).schema(schema).csv(csvPath)

  protected def writeDataFrameAsCsvToPath(dataFrame: DataFrame, csvPath: String): Unit =
    dataFrame.coalesce(1).write.format(csvFormat).option("sep", csvOutputDelimiter).mode(SaveMode.Overwrite).csv(csvPath)

}
