import axios from "axios";

export class UserService {
  constructor() {}

  async getMe() {
    const { data } = await axios.get("/api/user/me");
    return data;
  }
}
