import axios from 'axios'

export async function transformApex(apexCode, target = 'java') {
  const url = target === 'java' ? '/transform/apex-to-java' : '/transform/apex-to-js'
  const { data } = await axios.post(url, {
    apexCode,
    options: { useHerokuConnect: true, generateTests: true }
  })
  return data
}

export async function runJob(path, payload = {}) {
  const { data } = await axios.post(`/jobs/${path}/run`, payload)
  return data
}


