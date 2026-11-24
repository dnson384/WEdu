import { IVerificationRepository } from "@/domain/repositories/IVerificationRepository";

export class VerificationUsecase {
  constructor(private readonly repository: IVerificationRepository) {}
  metadata(token?: string) {
    return this.repository.metadata(token);
  }
  verify(otp: string, token?: string) {
    return this.repository.verify(otp, token);
  }
  resend(token?: string) {
    return this.repository.resend(token);
  }
  exchange(code: string, verifier: string) {
    return this.repository.exchange(code, verifier);
  }
}
