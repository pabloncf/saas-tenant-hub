export interface PageMeta {
  page: number;
  size: number;
  total: number;
  total_pages: number;
}

export interface ApiResponse<T> {
  data: T;
  meta?: PageMeta;
}
