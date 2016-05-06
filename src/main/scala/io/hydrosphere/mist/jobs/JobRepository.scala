package io.hydrosphere.mist.jobs

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.stream.ActorMaterializer
import io.hydrosphere.mist.actors.{TryRecoveyNext, JobComplited, JobRecovery, JobStarted}
import io.hydrosphere.mist._
import io.hydrosphere.mist.Constants.Actors.{asyncJobRunnerName}
import org.apache.commons.lang.SerializationUtils
import org.mapdb.{Serializer, DBMaker}

import io.hydrosphere.mist.jobs.JobStatus

import scala.collection.mutable.ArrayBuffer

private[mist] trait JobRepository extends Repository[Job]

private[mist] object InMemoryJobRepository extends JobRepository {

  private val _collection = ArrayBuffer.empty[Job]

  override def add(job: Job): Unit = {
    _collection += job
  }

  override def get(specification: Specification[Job]): Option[Job] = {
    val predicate: Job => Boolean = x => specification.specified(x)
    _collection.find(predicate)
  }

  override def filter(specification: Specification[Job]): List[Job] = {
    val predicate: Job => Boolean = x => specification.specified(x)
    _collection.filter(predicate).toList
  }

  override def remove(job: Job): Unit = {
    _collection -= job
  }
}

private[mist] object SQLiteJobRepository extends JobRepository {

  override def add(job: Job): Unit = ???

  override def get(specification: Specification[Job]): Option[Job] = ???

  override def filter(specification: Specification[Job]): List[Job] = ???

  override def remove(job: Job): Unit = ???
}

private[mist] trait ConfigurationRepository {
  def add(job: Job): Unit = ???
  def remove(job: Job): Unit = ???
  def getAll: ArrayBuffer[JobConfiguration] = ???
}

private [mist] object InMapDbJobConfigurationRepository extends ConfigurationRepository {
  // Db
  private lazy val db  =  DBMaker
    .fileDB(MistConfig.Recovery.recoveryDbFileName)
    .fileLockDisable
    .closeOnJvmShutdown
    .make

  // Map
  private lazy val map = db
    .hashMap("map", Serializer.STRING, Serializer.BYTE_ARRAY)
    .createOrOpen

  // Json formats
  private implicit val formats = org.json4s.DefaultFormats

  override def add(job: Job): Unit = {
    try {
      val w_job = SerializationUtils.serialize(job.configuration)
      map.put(job.id, w_job)
      println(s"${job.id} saved in MapDb")
    } catch {
      case e: Exception => println(e)
    }
  }

  override def remove(job: Job) = {
    try {
      map.remove(job.id)
      println(s"${job.id} removed from MapDb")
    } catch{
      case e: Exception => println(e)
    }
  }

  override def getAll: ArrayBuffer[JobConfiguration] = {
    try{
      val keys = map.getKeys.toArray()
      var _collection = ArrayBuffer.empty[JobConfiguration]
      for(key <- keys){
        _collection += SerializationUtils.deserialize(map.get(key.toString)).asInstanceOf[JobConfiguration]
      }
      println(s"${_collection.size} loaded from MapDb")
      _collection
    }
    catch {
      case e: Exception => {
        println(e)
        ArrayBuffer.empty[JobConfiguration]
      }
    }
  }
}

private[mist] object RecoveryJobRepository extends JobRepository {

  private val _collection = ArrayBuffer.empty[Job]

  private val configurationRepository = InMapDbJobConfigurationRepository

  override def add(job: Job): Unit = {
    _collection += job
    if(job.jobRunnerName == Constants.Actors.asyncJobRunnerName)
      configurationRepository.add(job)
    Mist.recoveryActor ! JobStarted
  }

  override def get(specification: Specification[Job]): Option[Job] = {
    val predicate: Job => Boolean = x => specification.specified(x)
    _collection.find(predicate)
  }
  override def filter(specification: Specification[Job]): List[Job] = {
    val predicate: Job => Boolean = x => specification.specified(x)
    _collection.filter(predicate).toList
  }
  override def remove(job: Job): Unit = {
    _collection -= job
    if(job.jobRunnerName == Constants.Actors.asyncJobRunnerName)
      configurationRepository.remove(job)
    Mist.recoveryActor ! JobComplited
  }

}