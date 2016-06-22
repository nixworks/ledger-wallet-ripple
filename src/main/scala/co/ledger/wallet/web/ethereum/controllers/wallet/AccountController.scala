package co.ledger.wallet.web.ethereum.controllers.wallet

import biz.enef.angulate.Module.RichModule
import biz.enef.angulate.core.JQLite
import biz.enef.angulate.{Controller, Scope}
import co.ledger.wallet.core.wallet.ethereum.Operation
import co.ledger.wallet.web.ethereum.components.SnackBar
import co.ledger.wallet.web.ethereum.i18n.DateFormat
import co.ledger.wallet.web.ethereum.services.{SessionService, WindowService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.util.{Failure, Success}
/**
  *
  * OperationController
  * ledger-wallet-ethereum-chrome
  *
  * Created by Pierre Pollastri on 03/05/2016.
  *
  * The MIT License (MIT)
  *
  * Copyright (c) 2016 Ledger
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in all
  * copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  *
  */
class AccountController(override val windowService: WindowService,
                        sessionService: SessionService,
                        $scope: Scope,
                        $element: JQLite,
                       $routeParams: js.Dictionary[String])
  extends Controller with WalletController {
  println(JSON.stringify($routeParams))

  val accountId = $routeParams("id").toInt

  def refresh(): Unit = {
    println("Refresh now!")
    SnackBar.success("Transaction completed", "Successfully broadcasted to network").show()
    isRefreshing = !isRefreshing
  }

  var isRefreshing = false

  var operations = js.Array[js.Dictionary[js.Any]]()

  private var reloadOperationNonce = 0
  def reloadOperations(): Unit = {
    reloadOperationNonce += 1
    val nonce = reloadOperationNonce
    sessionService.currentSession.get.wallet.account(accountId).flatMap {
      _.operations(-1, 10)
    } foreach {cursor =>
      var isLoading = false
      def loadMore(): Unit = {
        isLoading = true
        cursor.loadNextChunk() andThen {
          case Success(ops) =>
            ops foreach {(op) =>
              operations.push(js.Dictionary[js.Any](
                "date" -> DateFormat.formatStandard(op.transaction.receivedAt),
                "amount" -> ((if (op.`type` == Operation.SendType) "-" else "+") + op.transaction.value.toEther.toString()),
                "isSend" -> (op.`type` == Operation.SendType)
              ))
            }
            $scope.$digest()
          case Failure(ex) => ex.printStackTrace()
        } andThen {
          case all => isLoading = false
        }
      }

      def refresh() = {
        val top = $element.asInstanceOf[js.Dynamic].scrollTop().asInstanceOf[Double]
        val scrollHeight = $element.asInstanceOf[js.Dynamic].height().asInstanceOf[Double]
        val height = $element(0).asInstanceOf[js.Dynamic].scrollHeight.asInstanceOf[Double]
        if (top + scrollHeight >= height * 0.90) {
          if (!isLoading && reloadOperationNonce == nonce && cursor.loadedChunkCount < cursor.chunkCount) {
            loadMore()
          }
        }
      }

      $element.asInstanceOf[js.Dynamic].scroll({() =>
        refresh()
      })

      js.Dynamic.global.$(js.Dynamic.global.window).resize({() =>
        refresh()
      })

      loadMore()
    }
  }

 reloadOperations()
}

object AccountController {

  def init(module: RichModule) = {
    module.controllerOf[AccountController]("AccountController")
  }
  
}