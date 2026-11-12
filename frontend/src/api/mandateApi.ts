import { apiClient } from './client';
import type {
  ApiResponse,
  Order,
  PaymentAuditEntry,
  PaymentExecutionStatus,
  SpendMandate,
  SpendMandateRequest,
} from '../types';

export const mandateApi = {
  async getMyMandates(): Promise<SpendMandate[]> {
    const res = await apiClient.get<ApiResponse<SpendMandate[]>>('/mandates/me');
    return res.data.data || [];
  },

  async createMandate(payload: SpendMandateRequest): Promise<SpendMandate> {
    const res = await apiClient.post<ApiResponse<SpendMandate>>('/mandates', payload);
    return res.data.data;
  },

  async updateMandate(mandateId: string, payload: SpendMandateRequest): Promise<SpendMandate> {
    const res = await apiClient.put<ApiResponse<SpendMandate>>(`/mandates/${mandateId}`, payload);
    return res.data.data;
  },

  async revokeMandate(mandateId: string): Promise<SpendMandate> {
    const res = await apiClient.post<ApiResponse<SpendMandate>>(`/mandates/${mandateId}/revoke`);
    return res.data.data;
  },

  async getExecutionStatus(negotiationId: string): Promise<PaymentExecutionStatus> {
    const res = await apiClient.get<ApiResponse<PaymentExecutionStatus>>(
      `/payments/execution-status/${negotiationId}`
    );
    return res.data.data;
  },

  async executeDelegatedPayment(payload: {
    negotiationId: string;
    dealToken: string;
    shippingDetails: Record<string, string>;
  }): Promise<Order> {
    const res = await apiClient.post<ApiResponse<Order>>('/payments/delegated/execute', payload);
    return res.data.data;
  },

  async getMyAuditLogs(): Promise<PaymentAuditEntry[]> {
    const res = await apiClient.get<ApiResponse<PaymentAuditEntry[]>>('/payments/audit/my-logs');
    return res.data.data || [];
  },
};
