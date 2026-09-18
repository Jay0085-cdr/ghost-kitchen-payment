export type UserRole = "OWNER" | "STAFF";

export type ReportStatus = "PENDING" | "PARSED" | "FAILED";

export type ReconciliationRunStatus = "RUNNING" | "COMPLETED" | "FAILED";

export type ReconciliationResultStatus =
  | "MATCHED"
  | "UNDERPAID"
  | "OVERPAID"
  | "MISSING"
  | "UNEXPLAINED";

export type DiscrepancyCategory =
  | "COMMISSION"
  | "ADVERTISING_FEE"
  | "CANCELLATION_PENALTY"
  | "TAX_ADJUSTMENT"
  | "OTHER_DEDUCTION"
  | "MISSING_TRANSACTION"
  | "UNKNOWN";

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  userId: string;
  organizationId: string;
  email: string;
  role: UserRole;
}

export interface OrganizationResponse {
  id: string;
  name: string;
  createdAt: string;
}

export interface PlatformResponse {
  id: string;
  code: string;
  displayName: string;
}

export interface SettlementReportResponse {
  id: string;
  organizationId: string;
  platformCode: string;
  platformDisplayName: string;
  fileName: string;
  fileHash: string;
  uploadedAt: string;
  periodStart: string;
  periodEnd: string;
  status: ReportStatus;
  errorDetail: string | null;
  transactionCount: number;
}

export interface PlatformTransactionResponse {
  id: string;
  platformOrderId: string;
  orderDate: string;
  grossAmount: string;
  commission: string;
  advertisingFee: string;
  cancellationPenalty: string;
  taxAdjustment: string;
  otherDeduction: string;
  netExpectedPayout: string;
}

export interface BankTransactionResponse {
  id: string;
  txnDate: string;
  amount: string;
  narration: string | null;
  referenceNo: string | null;
  matched: boolean;
}

export interface BankStatementResponse {
  id: string;
  organizationId: string;
  fileName: string;
  fileHash: string;
  uploadedAt: string;
  status: ReportStatus;
  errorDetail: string | null;
  transactions: BankTransactionResponse[];
}

export interface ReconciliationRunResponse {
  id: string;
  organizationId: string;
  platformCode: string;
  platformDisplayName: string;
  periodStart: string;
  periodEnd: string;
  executedAt: string;
  status: ReconciliationRunStatus;
  totalResults: number;
  statusCounts: Partial<Record<ReconciliationResultStatus, number>>;
}

export interface ReconciliationResultResponse {
  id: string;
  reconciliationRunId: string;
  platformTransactionId: string | null;
  platformOrderId: string | null;
  bankTransactionId: string | null;
  expectedAmount: string | null;
  actualAmount: string | null;
  difference: string | null;
  status: ReconciliationResultStatus;
  explanation: string;
  createdAt: string;
}

export interface DiscrepancyResponse {
  id: string;
  reconciliationResultId: string;
  category: DiscrepancyCategory;
  expectedAmount: string;
  actualAmount: string;
  differenceAmount: string;
  notes: string | null;
  createdAt: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}
