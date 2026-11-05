import { PaymentOrder, PaymentPlan, PaymentReturnParameters } from "@/domain/entities/payment.entity";

export interface IPaymentRepository {
  plan(token: string): Promise<PaymentPlan>;
  create(key: string, token: string): Promise<PaymentOrder>;
  order(id: string, token: string): Promise<PaymentOrder>;
  verify(id: string, token: string): Promise<PaymentOrder>;
  confirmReturn(id: string, parameters: PaymentReturnParameters, token: string): Promise<PaymentOrder>;
}
