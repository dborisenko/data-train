package com.dbrsn.datatrain

import cats.Traverse
import cats.free.Free
import com.dbrsn.datatrain.dsl.meta.ResourceInject
import com.dbrsn.datatrain.model.Content
import com.dbrsn.datatrain.model.Resource
import com.dbrsn.datatrain.model.ResourceId
import shapeless._

import scala.language.higherKinds

trait BatchConvertComponent[Img, FileExisted, FileNotExisted, DirExisted, MetadataCollection[_], ConvertCollection[_]] {
  self: ConvertComponent[Img, FileExisted, FileNotExisted, DirExisted, MetadataCollection] =>

  trait BaseBatchConvert[F[_]] {
    type FreeF[A] = Free[F, A]

    def traverseConvertCollection(convertOps: ConvertCollection[Convert[F]])
      (input: FileExisted, resourceId: ResourceId)
      (implicit CT: Traverse[ConvertCollection]): FreeF[ConvertCollection[Content]] = {
      CT.traverse(convertOps) { convert =>
        val g: FreeF[Content] = convert(input, resourceId)
        g
      }
    }
  }

  case class BatchConvert[F[_]](
    convert: ConvertCollection[Convert[F]]
  )(implicit R: ResourceInject[F], CT: Traverse[ConvertCollection])
    extends ((FileExisted, String) => Free[F, Resource :: ConvertCollection[Content] :: HNil])
      with BaseBatchConvert[F] {

    override def apply(file: FileExisted, fileName: String): Free[F, Resource :: ConvertCollection[Content] :: HNil] = for {
      resource <- R.createResource(Resource(
        id = ResourceId.newResourceId,
        createdAt = clock.now
      ))
      result <- traverseConvertCollection(convert)(file, resource.id)
    } yield resource :: result :: HNil
  }

  case class NormalizedBatchConvert[F[_]](
    normalizer: Convert[F],
    convertFromNormalized: ConvertCollection[Convert[F]]
  )(implicit R: ResourceInject[F], CT: Traverse[ConvertCollection])
    extends ((FileExisted, String) => Free[F, Resource :: Content :: ConvertCollection[Content] :: HNil])
      with BaseBatchConvert[F] {

    override def apply(file: FileExisted, fileName: String): Free[F, Resource :: Content :: ConvertCollection[Content] :: HNil] = for {
      resource <- R.createResource(Resource(
        id = ResourceId.newResourceId,
        createdAt = clock.now
      ))
      input <- normalizer.applyWithoutDeleting(file, resource.id)
      result <- traverseConvertCollection(convertFromNormalized)(input.select[FileExisted], resource.id)
      _ <- normalizer.delete(input.select[FileExisted], input.select[DirExisted])
    } yield resource :: input.select[Content] :: result :: HNil
  }

}
