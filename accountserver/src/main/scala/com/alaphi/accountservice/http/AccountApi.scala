package com.alaphi.accountservice.http

import cats.implicits._
import cats.effect.{ConcurrentEffect, Sync}
import com.alaphi.accountservice.program.AccountAlgebra
import com.alaphi.accountservice.http.JsonCodec._
import com.alaphi.accountservice.model.Account.{AccountCreation, Deposit, MoneyTransfer}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

class AccountApi[F[_]: ConcurrentEffect](accountAlgebra: AccountAlgebra[F]) extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F]  {
    case GET -> Root / "accounts" / number =>
      accountAlgebra
        .read(number)
        .flatMap {
          _.fold(nf => NotFound(nf.description), acc => Ok(acc))
        }
        .handleErrorWith {
          case err => BadRequest(err.getMessage)
        }

    case GET -> Root / "accounts" =>
      accountAlgebra
        .readAll
        .flatMap(Ok(_))
        .handleErrorWith {
          case err => BadRequest(err.getMessage)
        }

    case req @ POST -> Root / "accounts" =>
      req.decode[AccountCreation] { accCreate =>
        accountAlgebra
          .create(accCreate)
          .flatMap(Created(_))
          .handleErrorWith {
            case err => BadRequest(err.getMessage)
          }
      }

    case req @ POST -> Root / "accounts" / number / "deposit" =>
      req.decode[Deposit] { deposit =>
        accountAlgebra
          .deposit(number, deposit)
          .flatMap {
            _.fold(nf => NotFound(nf.description), dep => Ok(dep))
          }
          .handleErrorWith {
            case err => BadRequest(err.getMessage)
          }
      }

    case req @ POST -> Root / "accounts" / number / "transfer" =>
      req.decode[MoneyTransfer] { moneyTransfer =>
        accountAlgebra
          .transfer(number, moneyTransfer)
          .flatMap {
            _.fold(nf => NotFound(nf.description), trans => Ok(trans))
          }
          .handleErrorWith {
            case err => BadRequest(err.getMessage)
          }
      }

  }

}
