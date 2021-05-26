export default {
  mappings: {
    dynamic: 'strict',
    properties: {
      username: {
        type: 'keyword',
        fields: {
          text: {
            type: 'text',
          },
        },
      },
      workId: {
        type: 'keyword',
      },
      query: {
        type: 'keyword',
        fields: {
          text: {
            type: 'text',
          },
        },
      },
      rating: {
        type: 'integer',
      },
      position: {
        type: 'integer',
      },
    },
  },
}
