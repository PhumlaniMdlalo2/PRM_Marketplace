import { api } from './client';

export const listStudentGroups = async () => {
  const { data } = await api.get('/student-groups');
  return data;
};

export const createStudentGroup = async (group) => {
  const { data } = await api.post('/student-groups', group);
  return data;
};

export const joinStudentGroup = async (groupId) => {
  await api.post(`/student-groups/${groupId}/membership`);
};

export const leaveStudentGroup = async (groupId) => {
  await api.delete(`/student-groups/${groupId}/membership`);
};

export const listStudentGroupPosts = async (groupId) => {
  const { data } = await api.get(`/student-groups/${groupId}/posts`);
  return data;
};

export const createStudentGroupPost = async (groupId, post) => {
  const { data } = await api.post(`/student-groups/${groupId}/posts`, post);
  return data;
};
