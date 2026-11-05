import { IPaymentRepository } from "@/domain/repositories/IPaymentRepository";
import { PaymentReturnParameters } from "@/domain/entities/payment.entity";

export class PaymentUsecase {
  constructor(private readonly repository: IPaymentRepository) {}
  plan(token: string) {
    return this.repository.plan(token);
  }
  create(key: string, token: string) {
    return this.repository.create(key, token);
  }
  order(id: string, token: string) {
    return this.repository.order(id, token);
  }
  verify(id: string, token: string) {
    return this.repository.verify(id, token);
  }
  confirmReturn(id: string, parameters: PaymentReturnParameters, token: string) {
    return this.repository.confirmReturn(id, parameters, token);
  }
}
