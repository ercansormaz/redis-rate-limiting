local key = KEYS[1]
local windowDurationMillis = tonumber(ARGV[1])

-- Increase key value and get current
local count = redis.call('INCR', key)

-- Set ttl value if first increment operation
if count == 1 then
  redis.call('PEXPIRE', key, windowDurationMillis)
end

return count