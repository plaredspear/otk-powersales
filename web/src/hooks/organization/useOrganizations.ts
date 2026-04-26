import { useQuery } from '@tanstack/react-query';
import { fetchOrganizations, type FetchOrganizationsParams } from '@/api/organization';

export function useOrganizations(params: FetchOrganizationsParams) {
  return useQuery({
    queryKey: ['admin', 'organizations', params.keyword, params.level],
    queryFn: () => fetchOrganizations(params),
  });
}
