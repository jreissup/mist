package io.hydrosphere.mist.master.models

import java.util.UUID

import io.hydrosphere.mist.core.CommonData.Action
import io.hydrosphere.mist.core.jvmjob.JobInfoData
import io.hydrosphere.mist.master.JobDetails


case class RunSettings(
  /** Context name that overrides endpoint context */
  contextId: Option[String]
)

object RunSettings {

  val Default = RunSettings(None)

}

/**
  * Request for starting job by name
  * New version api
  */
case class EndpointStartRequest(
  endpointId: String,
  parameters: Map[String, Any],
  externalId: Option[String] = None,
  runSettings: RunSettings = RunSettings.Default,
  id: String = UUID.randomUUID().toString
)

case class JobStartRequest(
  id: String,
  endpoint: JobInfoData,
  context: ContextConfig,
  parameters: Map[String, Any],
  source: JobDetails.Source,
  externalId: Option[String],
  action: Action = Action.Execute
)

case class JobStartResponse(id: String)

/**
  * Like JobStartRequest, but for async interfaces
  * (spray json not support default values)
  */
case class AsyncEndpointStartRequest(
  endpointId: String,
  parameters: Option[Map[String, Any]],
  externalId: Option[String],
  runSettings: Option[RunSettings]
) {

  def toCommon: EndpointStartRequest =
    EndpointStartRequest(
      endpointId,
      parameters.getOrElse(Map.empty),
      externalId,
      runSettings.getOrElse(RunSettings.Default))
}


case class DevJobStartRequest(
  fakeName: String,
  path: String,
  className: String,
  parameters: Map[String, Any],
  externalId: Option[String],
  workerId: Option[String],
  context: String
)

case class DevJobStartRequestModel(
  fakeName: String,
  path: String,
  className: String,
  parameters: Option[Map[String, Any]],
  externalId: Option[String],
  workerId: Option[String],
  context: String
) {

  def toCommon: DevJobStartRequest = {
    DevJobStartRequest(
      fakeName = fakeName,
      path = path,
      className = className,
      parameters = parameters.getOrElse(Map.empty),
      externalId = externalId,
      workerId = workerId,
      context = context
    )
  }
}
case class JobDetailsLink(
  jobId: String,
  source: JobDetails.Source,
  startTime: Option[Long] = None,
  endTime: Option[Long] = None,
  status: JobDetails.Status = JobDetails.Status.Initialized,
  endpoint: String,
  workerId: Option[String],
  createTime: Long = System.currentTimeMillis()
)